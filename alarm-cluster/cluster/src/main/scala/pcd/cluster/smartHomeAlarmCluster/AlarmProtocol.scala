package pcd.cluster.smartHomeAlarmCluster

import org.apache.pekko.actor.typed.ActorRef
import pcd.cluster.CborSerializable

object AlarmProtocol:

  enum Zone:
    case LivingRoom
    case Kitchen
    case Bedroom
    case Perimeter

  trait Command

  trait Message extends Command with CborSerializable
  case class PinEntered(pin: String, replyTo: ActorRef[DisplayMessage]) extends Message
  case class SelectZones(zones: Set[Zone], replyTo: ActorRef[DisplayMessage]) extends Message
  case class SensorTriggered(id: String) extends Message
  case class SensorIdentity(id: String, sensor: ActorRef[Sensor.Event]) extends Message
  
  case class SensorsUpdated(refs: Set[ActorRef[Sensor.Event]]) extends Command

  case class DisplayMessage(msg: String) extends CborSerializable

  enum Timeout extends Command:
    case ExitTimeout
    case EntryTimeout

  export Zone.*, Timeout.*
