package org.cubingusa.usnationals;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.JsonReader;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.msebera.android.httpclient.Header;

public class StageScheduleActivity extends AppCompatActivity {
    private static final String TAG = "StageSchedule";

    private boolean mIsAdmin = false;
    private final Map<String, List<LayoutContainer>> mStageColorToLayouts =
            new HashMap<>();

    private class LayoutContainer {
        public LinearLayout layout = null;
        public boolean canShowColor = true;
        public boolean canShowRound = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stage_schedule);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Uri uri = new Uri.Builder()
                .scheme("http")
                .authority(Constants.HOSTNAME)
                .appendPath("full_schedule")
                .build();

        AsyncHttpClient client = new AsyncHttpClient();
        client.get(uri.toString(), new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                LinearLayout layout = (LinearLayout) findViewById(R.id.schedule_container);
                if (layout != null) {
                    layout.removeAllViewsInLayout();
                }
                try {
                    parseJson(responseBody);
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                }
            }

            @Override
            public void onFailure(
                    int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.e(TAG, error.toString());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = MenuHandler.menuOptionIntent(this, item);
        if (intent == null) {
            return super.onOptionsItemSelected(item);
        }
        startActivity(intent);
        return true;
    }

    private void parseJson(byte[] responseBody) throws IOException {
        JsonReader reader =
                new JsonReader(new InputStreamReader(new ByteArrayInputStream(responseBody)));
        try {
            reader.beginObject();
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case "isAdmin":
                        mIsAdmin = reader.nextBoolean();
                        break;
                    case "heats":
                        parseHeats(reader);
                        break;
                    default:
                        reader.skipValue();
                        break;
                }
            }
        } finally {
            reader.close();
        }
        final Map<String, Integer> stageNameToBorderBackground = new HashMap<>();
        stageNameToBorderBackground.put("Blue", R.drawable.border_blue);
        stageNameToBorderBackground.put("Green", R.drawable.border_green);
        stageNameToBorderBackground.put("Orange", R.drawable.border_orange);
        stageNameToBorderBackground.put("Red", R.drawable.border_red);
        stageNameToBorderBackground.put("Yellow", R.drawable.border_yellow);

        final Map<String, Integer> stageNameToColorBackground = new HashMap<>();
        stageNameToColorBackground.put("Blue", R.drawable.solid_blue);
        stageNameToColorBackground.put("Green", R.drawable.solid_green);
        stageNameToColorBackground.put("Orange", R.drawable.solid_orange);
        stageNameToColorBackground.put("Red", R.drawable.solid_red);
        stageNameToColorBackground.put("Yellow", R.drawable.solid_yellow);

        View spinner = findViewById(R.id.schedule_spinner);
        if (spinner != null) {
            spinner.setVisibility(View.GONE);
        }
        LinearLayout stageChipContainer = (LinearLayout) findViewById(R.id.stage_chip_container);
        if (stageChipContainer == null) {
            return;
        }
        int itemsAdded = 0;

        for (final String stageName : mStageColorToLayouts.keySet()) {
            getLayoutInflater().inflate(R.layout.content_stage_chip, stageChipContainer);
            final LinearLayout stageChip = (LinearLayout) stageChipContainer.getChildAt(itemsAdded);
            itemsAdded++;
            TextView stageChipText = (TextView) stageChip.findViewById(R.id.stage_chip_content);
            stageChipText.setText(stageName);
            final int borderBackground = stageNameToBorderBackground.get(stageName);
            final int colorBackground = stageNameToColorBackground.get(stageName);
            stageChip.setBackgroundResource(colorBackground);
            stageChip.setOnClickListener(new View.OnClickListener() {
                private boolean isActive = true;
                @Override
                public void onClick(View v) {
                    if (isActive) {
                        toggleChips(stageName, false);
                        stageChip.setBackgroundResource(borderBackground);
                    } else {
                        toggleChips(stageName, true);
                        stageChip.setBackgroundResource(colorBackground);
                    }
                    isActive = !isActive;
                }
            });
        }

    }

    private void parseHeats(JsonReader reader) throws IOException {
        reader.beginArray();
        LinearLayout scheduleContainer = (LinearLayout) findViewById(R.id.schedule_container);
        if (scheduleContainer == null) {
            reader.endArray();
            return;
        }
        ScheduleParser scheduleParser =
                new ScheduleParser(this, getLayoutInflater(), scheduleContainer);
        final Map<String, List<LayoutContainer>> roundToLayouts = new HashMap<>();
        while (reader.hasNext()) {
            Pair<Heat, LinearLayout> heatAndLayout = scheduleParser.parseHeat(reader);
            final Heat heat = heatAndLayout.first;
            LayoutContainer layoutContainer = new LayoutContainer();
            layoutContainer.layout = heatAndLayout.second;
            if (heat.number > 0) {
                layoutContainer.canShowRound = false;
                layoutContainer.layout.setVisibility(View.GONE);
                final String roundStageKey = heat.round.id + "_" + heat.stage.id;
                if (!roundToLayouts.containsKey(roundStageKey)) {
                    roundToLayouts.put(roundStageKey, new ArrayList<LayoutContainer>());
                    final LinearLayout container = scheduleParser.addScheduleItem(
                            heat, heat.event.name, heat.event, heat.stage.color);
                    final LayoutContainer containerContainer = new LayoutContainer();
                    containerContainer.layout = container;
                    containerContainer.canShowRound = true;
                    container.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            container.setVisibility(View.GONE);
                            for (LayoutContainer child : roundToLayouts.get(roundStageKey)) {
                                child.canShowRound = true;
                                if (child.canShowColor && child.canShowRound) {
                                    child.layout.setVisibility(View.VISIBLE);
                                } else {
                                    child.layout.setVisibility(View.GONE);
                                }
                            }
                            containerContainer.canShowRound = false;
                        }
                    });
                    if (!mStageColorToLayouts.containsKey(heat.stage.name)) {
                        mStageColorToLayouts.put(heat.stage.name, new ArrayList<LayoutContainer>());
                    }
                    mStageColorToLayouts.get(heat.stage.name).add(containerContainer);
                }
                roundToLayouts.get(roundStageKey).add(layoutContainer);
            }
            if (!mStageColorToLayouts.containsKey(heat.stage.name)) {
                mStageColorToLayouts.put(heat.stage.name, new ArrayList<LayoutContainer>());
            }
            mStageColorToLayouts.get(heat.stage.name).add(layoutContainer);
        }
        reader.endArray();
    }

    private void toggleChips(String stageName, boolean isVisible) {
        List<LayoutContainer> chips = mStageColorToLayouts.get(stageName);
        for (LayoutContainer chip : chips) {
            chip.canShowColor = isVisible;
            if (chip.canShowColor && chip.canShowRound) {
                chip.layout.setVisibility(View.VISIBLE);
            } else {
                chip.layout.setVisibility(View.GONE);
            }
        }
    }
}

