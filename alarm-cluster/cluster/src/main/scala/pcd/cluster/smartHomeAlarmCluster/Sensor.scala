package pcd.cluster.smartHomeAlarmCluster

import org.apache.pekko.actor.typed.scaladsl.*
import org.apache.pekko.actor.typed.*
import org.apache.pekko.actor.typed.receptionist.{Receptionist, ServiceKey}
import pcd.cluster.CborSerializable
import AlarmProtocol.*

import scala.concurrent.duration.*

object Sensor:
  val SensorKey: ServiceKey[Event] = ServiceKey[Event]("smart-home-sensor")
  trait Event extends CborSerializable
  private case object Trigger extends Event
  final case class SwitchOn(replyTo: ActorRef[Message]) extends Event
  case object SwitchOff extends Event
  final case class AskRef(replyTo: ActorRef[SensorIdentity]) extends Event

  case object StartScenario extends Event

  def apply(id: String, period: FiniteDuration): Behavior[Event] =
    Behaviors.setup: ctx =>
      Behaviors.withTimers: timers =>
        timers.startTimerAtFixedRate(Trigger, period)
        ctx.system.receptionist ! Receptionist.Register(SensorKey, ctx.self)
        off(ctx, id)

  private def log(s: String): Unit = println(s)

  private def off(ctx: ActorContext[Event], id: String): Behavior[Event] =
    Behaviors.receiveMessagePartial:
      case SwitchOn(replyTo) =>
        log(s"is now ON.")
        on(ctx, id, replyTo)
      case AskRef(replyTo) =>
        replyTo ! SensorIdentity(id, ctx.self)
        Behaviors.same

  private def on(ctx: ActorContext[Event], id: String, replyTo: ActorRef[Message]): Behavior[Event] =
    Behaviors.receiveMessagePartial:
      case Trigger =>
        log(s"triggered")
        replyTo ! SensorTriggered(id)
        Behaviors.same
      case SwitchOff =>
        log(s"is now OFF.")
        off(ctx, id)
      case AskRef(r) =>
        r ! SensorIdentity(id, ctx.self)
        Behaviors.same
