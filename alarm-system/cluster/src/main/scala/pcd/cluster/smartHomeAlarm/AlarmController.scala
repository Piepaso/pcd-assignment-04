package pcd.cluster.smartHomeAlarm

import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.*

import scala.concurrent.duration.*

object AlarmController:
  import AlarmProtocol.*

  private val correctPin = "123"
  private val exitDelayDuration = 5.seconds
  private val entryDelayDuration = 5.seconds

  def apply(): Behavior[Command] = Behaviors.setup: ctx =>
    Behaviors.withTimers: timers =>
      disarmed(Zone.values.toSet, timers, ctx)

  private def createState(ctx: ActorContext[Command], behavior: Behavior[Command]): Behavior[Command] =
    Behaviors.receiveMessage:
      case PinEntered(pin) if pin != correctPin =>
        ctx.log.warn("Invalid PIN entered.")
        Behaviors.same
      case msg => Behavior.interpretMessage(behavior, ctx, msg)

  private def disarmed(zonesToArm: Set[Zone], timers: TimerScheduler[Command], ctx: ActorContext[Command]): Behavior[Command] =
    createState(ctx, Behaviors.receiveMessagePartial:
      case PinEntered(_) =>
        ctx.log.info("Correct PIN entered. Starting exit delay...")
        timers.startSingleTimer(ExitTimeout, ExitTimeout, exitDelayDuration)
        exitDelay(zonesToArm, timers, ctx)

      case SelectZones(zones) =>
        ctx.log.info(s"Selected zones updated for next arming: ${zones.mkString(", ")}")
        disarmed(zones, timers, ctx)
    )

  private def exitDelay(zonesToArm: Set[Zone], timers: TimerScheduler[Command], ctx: ActorContext[Command]): Behavior[Command] =
    createState(ctx, Behaviors.receiveMessagePartial:
      case ExitTimeout =>
        ctx.log.info(s"Exit delay timed out. System ARMED for zones: ${zonesToArm.mkString(", ")}")
        armed(zonesToArm, timers, ctx)

      case PinEntered(_) =>
        ctx.log.info("Correct PIN entered. Exit delay cancelled. System remains disarmed.")
        timers.cancel(ExitTimeout)
        disarmed(zonesToArm, timers, ctx)

      case SensorTriggered(zone) =>
        ctx.log.info(s"Sensor triggered in zone [$zone] during exit delay. Event ignored.")
        Behaviors.same
    )

  private def armed(zonesToArm: Set[Zone], timers: TimerScheduler[Command], ctx: ActorContext[Command]): Behavior[Command] =
    createState(ctx, Behaviors.receiveMessagePartial:
      case SensorTriggered(zone) if zonesToArm.contains(zone) =>
        ctx.log.warn(s"INTRUSION DETECTED in active zone [$zone]! Starting entry delay...")
        timers.startSingleTimer(EntryTimeout, EntryTimeout, entryDelayDuration)
        entryDelay(zonesToArm, timers, ctx)

      case SensorTriggered(zone) =>
        ctx.log.info(s"Sensor triggered in inactive zone [$zone]. Event ignored.")
        Behaviors.same

      case PinEntered(_) =>
        ctx.log.info("Correct PIN entered. System disarmed successfully.")
        disarmed(zonesToArm, timers, ctx)
    )

  private def entryDelay(zonesToArm: Set[Zone], timers: TimerScheduler[Command], ctx: ActorContext[Command]): Behavior[Command] =
    createState(ctx, Behaviors.receiveMessagePartial:
      case PinEntered(_) =>
        ctx.log.info("Correct PIN entered. Alarm deactivated during entry delay.")
        timers.cancel(EntryTimeout)
        disarmed(zonesToArm, timers, ctx)

      case EntryTimeout =>
        ctx.log.error("Entry delay timed out! EMERGENCY: Activating alarm!")
        alarmActive(zonesToArm, timers, ctx)

      case SensorTriggered(zone) =>
        ctx.log.info(s"Sensor triggered in zone [$zone] during entry delay. Countdown already running.")
        Behaviors.same
    )

  private def alarmActive(zonesToArm: Set[Zone], timers: TimerScheduler[Command], ctx: ActorContext[Command]): Behavior[Command] =
    ctx.log.error("ALARM! Digit the correct PIN to disarm.")
    createState(ctx, Behaviors.receiveMessagePartial:
      case PinEntered(_) =>
        ctx.log.info("Correct PIN entered. Alarm stopped. System disarmed.")
        disarmed(zonesToArm, timers, ctx)
    )