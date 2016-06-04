package org.cubingusa.usnationals;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class ScheduleNotificationMessagingService extends FirebaseMessagingService {
    private final EventIcons mIcons;
    private int mNextNotificationId = 0;
    private Map<String, Integer> mHeatAssignmentToNotificationId = new HashMap<>();

    private NotificationManager mNotificationManager;

    public ScheduleNotificationMessagingService() {
        mIcons = new EventIcons(this);
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Map<String, String> data = message.getData();
        switch (data.get("type")) {
            case "heatNotification":
                String heatAssignmentId = data.get("heatAssignmentId");
                String eventId = data.get("eventId");
                String eventName = data.get("eventName");
                String competitorName = data.get("competitorName");
                String competitorId = data.get("competitorId");
                int heatNumber = Integer.parseInt(data.get("heatNumber"));
                String stageName = data.get("stageName");

                StringBuffer titleBuffer = new StringBuffer()
                        .append(eventName)
                        .append(" on the ")
                        .append(stageName)
                        .append(" stage");

                StringBuffer contentBuffer = new StringBuffer()
                        .append("It's time for ")
                        .append(competitorName)
                        .append(" to compete in ")
                        .append(eventName)
                        .append(" heat ")
                        .append(heatNumber)
                        .append(" on the ")
                        .append(stageName)
                        .append(" stage!");

                Intent notificationClickIntent = new Intent();
                notificationClickIntent.setClass(this, ScheduleActivity.class);
                notificationClickIntent.putExtra(ScheduleActivity.COMPETITOR_EXTRA, competitorId);
                PendingIntent notificationClickPendingIntent =
                        PendingIntent.getActivity(
                                this,
                                0,
                                notificationClickIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT);

                Uri ringtoneUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Bitmap largeIcon = BitmapFactory.decodeResource(
                        getResources(), mIcons.getDrawableId(eventId));
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                        .setContentTitle(titleBuffer.toString())
                        .setContentText(contentBuffer.toString())
                        .setSmallIcon(mIcons.getTransparentDrawableId(eventId))
                        .setLargeIcon(largeIcon)
                        .setAutoCancel(true)
                        .setContentIntent(notificationClickPendingIntent)
                        .setSound(ringtoneUri);

                mHeatAssignmentToNotificationId.put(heatAssignmentId, mNextNotificationId);
                mNotificationManager.notify(mNextNotificationId, builder.build());
                mNextNotificationId++;
                break;

            case "cancelHeatNotification":
                String heatAssignment = data.get("heatAssignmentId");
                if (!mHeatAssignmentToNotificationId.containsKey(heatAssignment)) {
                    return;
                }
                mNotificationManager.cancel(mHeatAssignmentToNotificationId.get(heatAssignment));
                break;
        }
    }

}
