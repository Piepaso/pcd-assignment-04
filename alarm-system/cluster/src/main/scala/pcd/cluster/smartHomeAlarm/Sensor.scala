package pcd.cluster.smartHomeAlarm

import org.apache.pekko.actor.typed.scaladsl.*
import org.apache.pekko.actor.typed.*
import pcd.cluster.smartHomeAlarm.AlarmProtocol.*

object Sensor:
  
  enum Event:
    case Trigger
    case Toggle

  export Event.*

  def apply(id: String, zone: Zone, controller: ActorRef[Command], on: Boolean = true): Behavior[Event] =
    Behaviors.receiveMessagePartial:
      case Trigger if on =>
        controller ! SensorTriggered(zone)
        Behaviors.same
      case Toggle => apply(id, zone, controller, !on)