package pcd.cluster.smartHomeAlarmCluster

import org.apache.pekko.actor.typed.*
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import AlarmProtocol.*

object Keypad:

  enum Event:
    case TypeDigit(digit: Char)
    case SelectZone(zone: Zone)
    case Enter
    case Message(msg: String)

  export Event.*

  def apply(controller: ActorRef[Command]): Behavior[Event] = Behaviors.setup: ctx =>
    val messageAdapter = ctx.messageAdapter[AlarmProtocol.DisplayMessage](m => Message(m.msg))
    ScenarioScript.keypadSteps.foreach(step => ctx.scheduleOnce(step.delay, ctx.self, step.msg))
    idle(controller, messageAdapter)

  private def log(s: String): Unit = println(s)

  private def idle(controller: ActorRef[Command], replyTo: ActorRef[AlarmProtocol.DisplayMessage]): Behavior[Event] =
    Behaviors.receiveMessagePartial:
      case TypeDigit(digit) if digit.isDigit =>
        typingPin(controller, digit.toString, replyTo)
      case SelectZone(zone) =>
        selectingZones(controller, Set(zone), replyTo)
      case Message(msg) =>
        log(msg)
        Behaviors.same

  private def typingPin(controller: ActorRef[Command], currentPin: String, replyTo: ActorRef[AlarmProtocol.DisplayMessage]): Behavior[Event] =
    Behaviors.receiveMessagePartial:
      case TypeDigit(digit) if digit.isDigit =>
        typingPin(controller, currentPin + digit, replyTo)
      case Enter =>
        controller ! AlarmProtocol.PinEntered(currentPin, replyTo)
        log(s"PIN entered: $currentPin")
        idle(controller, replyTo)

  private def selectingZones(controller: ActorRef[Command], currentZones: Set[Zone], replyTo: ActorRef[AlarmProtocol.DisplayMessage]): Behavior[Event] =
    Behaviors.receiveMessagePartial:
      case SelectZone(zone) =>
        val updatedZones = if currentZones.contains(zone) then currentZones - zone else currentZones + zone
        selectingZones(controller, updatedZones, replyTo)
      case Enter =>
        controller ! AlarmProtocol.SelectZones(currentZones, replyTo)
        log(s"Zones selected: ${currentZones.mkString(", ")}")
        idle(controller, replyTo)
