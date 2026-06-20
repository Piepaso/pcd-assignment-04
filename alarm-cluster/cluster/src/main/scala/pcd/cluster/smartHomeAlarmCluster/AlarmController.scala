package pcd.cluster.smartHomeAlarmCluster

import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.*
import org.apache.pekko.actor.typed.receptionist.Receptionist
import AlarmProtocol.*

import scala.concurrent.duration.*

type SensorRef = Map[String, ActorRef[Sensor.Event]]

class AlarmController(correctPin: String, sensorPositions: Map[String, Zone]):

  private val exitDelayDuration = 5.seconds
  private val entryDelayDuration = 5.seconds

  def apply(): Behavior[Command] = Behaviors.setup: ctx =>
    Behaviors.withTimers: timers =>
      val listingAdapter = ctx.messageAdapter[Receptionist.Listing](listing =>
        SensorsUpdated(listing.serviceInstances(Sensor.SensorKey))
      )
      ctx.system.receptionist ! Receptionist.Subscribe(Sensor.SensorKey, listingAdapter)
      log("Started/recreated -> entering RECOVERY MODE (state unknown).")
      recovery(Map.empty, timers, ctx)

  private def createState(
                           ctx: ActorContext[Command],
                           sensorRefs: SensorRef,
                           rebuild: SensorRef => Behavior[Command],
                           behavior: Behavior[Command]
                         ): Behavior[Command] = Behaviors.receiveMessage:
    case SensorsUpdated(refs) =>
      val filteredSensorRef = sensorRefs.filter((_, v) => refs.contains(v))
      val known = filteredSensorRef.values.toSet
      refs.diff(known).foreach(_ ! Sensor.AskRef(ctx.self))
      rebuild(filteredSensorRef)
    case SensorIdentity(id, newSensorRef) =>
      if !sensorRefs.contains(id) then log(s"Discovered sensor [$id].")
      rebuild(sensorRefs.updated(id, newSensorRef))
    case PinEntered(pin, replyTo) if pin != correctPin =>
      replyTo ! DisplayMessage("Invalid PIN entered.")
      Behaviors.same
    case msg => Behavior.interpretMessage(behavior, ctx, msg)

  private def messageToSensors(sensors: SensorRef, zonesToArm: Set[Zone], message: Sensor.Event): Unit =
    sensors.foreach((id, ref) =>
      if zonesToArm.contains(sensorPositions(id)) then ref ! message
    )

  private def log(m: String): Unit = println(m)

  /* Safe state entered on every (re)start. The previous state is unknown, so
   sensor events are only logged and the only way forward is a correct PIN. */
  private def recovery(sensors: SensorRef, timers: TimerScheduler[Command], ctx: ActorContext[Command]): Behavior[Command] =
    createState(ctx, sensors, s => recovery(s, timers, ctx),
      Behaviors.receiveMessagePartial:
        case PinEntered(_, replyTo) =>
          replyTo ! DisplayMessage("System recovered and disarmed.")
          log("Correct PIN in recovery -> system DISARMED.")
          disarmed(sensors, Zone.values.toSet, timers, ctx)

        case SensorTriggered(id) =>
          log(s"Sensor [$id] in zone [${sensorPositions(id)}] triggered while in RECOVERY mode. Event ignored.")
          Behaviors.same

        case SelectZones(_, replyTo) =>
          replyTo ! DisplayMessage("System in recovery: enter the PIN first.")
          Behaviors.same
    )

  private def disarmed(sensors: SensorRef, zonesToArm: Set[Zone], timers: TimerScheduler[Command], ctx: ActorContext[Command]): Behavior[Command] =
    createState(ctx, sensors, s => disarmed(s, zonesToArm, timers, ctx),
      Behaviors.receiveMessagePartial:
        case PinEntered(_, replyTo) =>
          replyTo ! DisplayMessage("Starting exit delay...")
          timers.startSingleTimer(ExitTimeout, ExitTimeout, exitDelayDuration)
          exitDelay(sensors, zonesToArm, timers, ctx)

        case SelectZones(zones, replyTo) =>
          replyTo ! DisplayMessage(s"Selected zones updated for next arming: ${zones.mkString(", ")}")
          disarmed(sensors, zones, timers, ctx)
    )

  private def exitDelay(sensors: SensorRef, zonesToArm: Set[Zone], timers: TimerScheduler[Command], ctx: ActorContext[Command]): Behavior[Command] =
    createState(ctx, sensors, s => exitDelay(s, zonesToArm, timers, ctx),
      Behaviors.receiveMessagePartial:
        case ExitTimeout =>
          log(s"Exit delay timed out. System ARMED for zones: ${zonesToArm.mkString(", ")}")
          messageToSensors(sensors, zonesToArm, Sensor.SwitchOn(ctx.self))
          armed(sensors, zonesToArm, timers, ctx)

        case PinEntered(_, replyTo) =>
          replyTo ! DisplayMessage("Exit delay cancelled. System remains disarmed.")
          timers.cancel(ExitTimeout)
          disarmed(sensors, zonesToArm, timers, ctx)

        case SensorTriggered(id) =>
          log(s"Sensor triggered in zone [${sensorPositions(id)}] during exit delay. Event ignored.")
          Behaviors.same
    )

  private def armed(sensors: SensorRef, zonesToArm: Set[Zone], timers: TimerScheduler[Command], ctx: ActorContext[Command]): Behavior[Command] =
    createState(ctx,
      sensors,
      s =>
        messageToSensors(s, zonesToArm, Sensor.SwitchOn(ctx.self))
        armed(s, zonesToArm, timers, ctx),
      Behaviors.receiveMessagePartial:
        case SensorTriggered(id) if zonesToArm.contains(sensorPositions(id))  =>
          log(s"INTRUSION DETECTED in active zone [${sensorPositions(id)}]! Starting entry delay...")
          timers.startSingleTimer(EntryTimeout, EntryTimeout, entryDelayDuration)
          entryDelay(sensors, zonesToArm, timers, ctx)

        case PinEntered(_, replyTo) =>
          replyTo ! DisplayMessage("System disarmed successfully.")
          disarmed(sensors, zonesToArm, timers, ctx)
    )

  private def entryDelay(sensors: SensorRef, zonesToArm: Set[Zone], timers: TimerScheduler[Command], ctx: ActorContext[Command]): Behavior[Command] =
    createState(ctx,
      sensors,
      s =>
        messageToSensors(s, zonesToArm, Sensor.SwitchOn(ctx.self))
        entryDelay(s, zonesToArm, timers, ctx),
      Behaviors.receiveMessagePartial:
        case PinEntered(_, replyTo) =>
          replyTo ! DisplayMessage("Alarm deactivated during entry delay.")
          messageToSensors(sensors, zonesToArm, Sensor.SwitchOff)
          timers.cancel(EntryTimeout)
          disarmed(sensors, zonesToArm, timers, ctx)

        case EntryTimeout =>
          log(s"Entry delay timed out! EMERGENCY: Activating alarm!")
          alarmActive(sensors, zonesToArm, timers, ctx)
    )

  private def alarmActive(sensors: SensorRef, zonesToArm: Set[Zone], timers: TimerScheduler[Command], ctx: ActorContext[Command]): Behavior[Command] =
    log(s"ALARM! Digit the correct PIN to disarm.")
    createState(ctx,
      sensors,
      s =>
        messageToSensors(s, zonesToArm, Sensor.SwitchOn(ctx.self))
        entryDelay(s, zonesToArm, timers, ctx),
      Behaviors.receiveMessagePartial:
        case PinEntered(_, replyTo) =>
          replyTo ! DisplayMessage("Alarm deactivated.")
          messageToSensors(sensors, zonesToArm, Sensor.SwitchOff)
          disarmed(sensors, zonesToArm, timers, ctx)

        case SensorTriggered(id) =>
          log(s"Sensor triggered in zone [${sensorPositions(id)}].")
          Behaviors.same
    )
