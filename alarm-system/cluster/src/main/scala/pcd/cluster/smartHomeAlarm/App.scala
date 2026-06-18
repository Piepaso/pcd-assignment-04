package pcd.cluster.smartHomeAlarm

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Behavior}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.Random
import AlarmProtocol.Zone.*

object App:

  private case class Event[E](delay: FiniteDuration, device: ActorRef[E], msg: E)

  def apply(): Behavior[Unit] = Behaviors.setup: context =>

    def simulatePhysicalEvent[E] (e: Event[E]): Unit = e match
      case Event(delay, device, msg) =>
        val _ = context.spawn(
          Behaviors.withTimers[Unit]: timers =>
            timers.startSingleTimer((), delay)
            Behaviors.receiveMessage: _ =>
              device ! msg
              Behaviors.stopped
          ,
          Random().nextInt().toString
        )

    val controller = context.spawn(AlarmController(), "alarm-controller")
    val keypad = context.spawn(Keypad(controller), "keypad")
    val door = context.spawn(Sensor("door", LivingRoom, controller), "door-sensor")
    val _ = context.spawn(Sensor("window", LivingRoom, controller), "window-sensor")
    val _ = context.spawn(Sensor("motion", Perimeter, controller), "motion-sensor")
    val _ = context.spawn(Sensor("smoke", Kitchen, controller), "smoke-sensor")
    val bedroom = context.spawn(Sensor("bedroom-window", Bedroom, controller), "bedroom-window-sensor")

    List(
      Event(1.seconds, keypad, Keypad.TypeDigit('1')),
      Event(2.seconds, keypad, Keypad.TypeDigit('2')),
      Event(3.seconds, keypad, Keypad.TypeDigit('3')),
      Event(4.seconds, keypad, Keypad.Enter),
      Event(6.seconds, door, Sensor.Trigger),
      Event(10.seconds, bedroom, Sensor.Trigger),
      Event(11.seconds, keypad, Keypad.TypeDigit('1')),
      Event(12.seconds, keypad, Keypad.TypeDigit('2')),
      Event(13.seconds, keypad, Keypad.TypeDigit('3')),
      Event(14.seconds, keypad, Keypad.Enter),
      Event(15.seconds, keypad, Keypad.SelectZone(LivingRoom)),
      Event(15.seconds, keypad, Keypad.SelectZone(Kitchen)),
      Event(16.seconds, keypad, Keypad.Enter),
      Event(17.seconds, keypad, Keypad.TypeDigit('1')),
      Event(18.seconds, keypad, Keypad.TypeDigit('2')),
      Event(19.seconds, keypad, Keypad.TypeDigit('3')),
      Event(20.seconds, keypad, Keypad.Enter),
      Event(26.seconds, bedroom, Sensor.Trigger),
      Event(27.seconds, door, Sensor.Trigger),
      Event(32.seconds, keypad, Keypad.TypeDigit('1')),
      Event(33.seconds, keypad, Keypad.TypeDigit('2')),
      Event(34.seconds, keypad, Keypad.TypeDigit('3')),
      Event(35.seconds, keypad, Keypad.Enter),
    ).foreach(simulatePhysicalEvent)

    Behaviors.empty

  @main def run(): Unit =
    println("Starting Smart Home Alarm system ...")
    val _ = ActorSystem(App(), "SmartHomeSystem")