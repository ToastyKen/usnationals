import collections
import json
import webapp2

from src.models import AdminDevice
from src.models import Heat

class GetStageSchedule(webapp2.RequestHandler):
  def get(self, stages):
    device_id = self.request.get('device_id')
    if device_id:
      device = AdminDevice.get_by_id(device_id)
      is_admin = device and device.is_authorized
    else:
      is_admin = False
    output_dict = {
        'is_admin': is_admin,
        'heats': [],
    }
    heats = Heat.query().iter()
    heats_by_time = collections.defaultdict(list)
    for heat in heats:
      if heat.stage.id() in stages:
        heats_by_time[heat.start_time].append(heat)
    for time in sorted(heats_by_time):
      for heat in heats_by_time[time]:
        output_dict['heats'].append(heat.ToDict())
    self.response.write(json.dumps(output_dict))