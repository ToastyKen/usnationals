from google.appengine.ext import ndb

from src.models.competitor import Competitor
from src.models.heat import Heat
from src.models.round import Round

class StaffAssignment(ndb.Model):
  heat = ndb.KeyProperty(kind=Heat)
  long_event = ndb.KeyProperty(kind=Round)
  staff_member = ndb.KeyProperty(kind=Competitor)
  job = ndb.StringProperty()
  station = ndb.IntegerProperty()
  misc = ndb.StringProperty()

  @staticmethod
  def Id(event_id, round_id, stage, heat_number, competitor_id):
    return '%s_%s' % (Heat.Id(event_id, round_id, stage, heat_number), competitor_id)

  def ToDict(self):
    output = {
        'heat': self.heat.get().ToDict(),
        'staff_member': self.staff_member.get().ToDict(),
        'job': self.job,
    }
    if self.station:
      output['station'] = self.station
    if self.long_event:
      output['long_event'] = self.long_event.get().ToDict()
    if self.misc:
      output['misc'] = self.misc
    return output
