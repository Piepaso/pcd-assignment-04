package pcd.cluster.smartHomeAlarm

import org.apache.pekko.actor.typed.*
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import pcd.cluster.smartHomeAlarm.AlarmProtocol.*

object Keypad:
  enum Event:
    case TypeDigit(digit: Char)
    case SelectZone(zone: Zone)
    case Enter

  export Event.*

  def apply(controller: ActorRef[Command]): Behavior[Event] =
    idle(controller)

  private def idle(controller: ActorRef[Command]): Behavior[Event] = Behaviors.receiveMessagePartial:
    case TypeDigit(digit) if digit.isDigit =>
      typingPin(controller, digit.toString)
    case SelectZone(zone) =>
      selectingZones(controller, Set(zone))

  private def typingPin(controller: ActorRef[Command], currentPin: String): Behavior[Event] = Behaviors.receiveMessagePartial:
    case TypeDigit(digit) if digit.isDigit =>
      typingPin(controller, currentPin + digit)
    case Enter =>
      controller ! AlarmProtocol.PinEntered(currentPin)
      idle(controller)

  private def selectingZones(controller: ActorRef[Command], currentZones: Set[Zone]): Behavior[Event] = Behaviors.receiveMessagePartial:
    case SelectZone(zone) =>
      val updatedZones = if currentZones.contains(zone) then currentZones - zone else currentZones + zone
      selectingZones(controller, updatedZones)
    case Enter =>
      controller ! AlarmProtocol.SelectZones(currentZones)
      idle(controller)