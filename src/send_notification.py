import webapp2

from src import firebase
from src.models import Heat
from src.models import HeatAssignment

class SendNotification(webapp2.RequestHandler):
  # TODO: change this to post
  def get(self, event_id, round_id, stage_id, heat_number):
    round_id = int(round_id)
    heat_number = int(heat_number)
    heat_id = Heat.Id(event_id, round_id, stage_id, heat_number)
    heat = Heat.get_by_id(heat_id)
    if not heat:
      self.response.set_status(400)
      self.response.write('No heat found for ' + heat_id)
      return
    event = heat.round.get().event.get()
    stage = heat.stage.get()

    # TODO: record the time, and don't send notifications too frequently
    for heat_assignment in HeatAssignment.query(HeatAssignment.heat == heat.key).iter():
      competitor = heat_assignment.competitor.get()
      data = {"type": "heatNotification",
              "heatAssignmentId": heat_assignment.key.id(),
              "eventId": event.key.id(),
              "eventName": event.name,
              "competitorName": competitor.name,
              "competitorId": competitor.key.id(),
              "heatNumber": heat_number,
              "stageName": stage.name}
      topic = "/topics/competitor_" + competitor.key.id()
      firebase.SendPushNotification(topic, data, "heatNotification")
