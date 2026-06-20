package pcd.cluster.smartHomeAlarmCluster

import scala.concurrent.duration.*

object ScenarioScript:

  final case class KeypadStep(delay: FiniteDuration, msg: Keypad.Event)

  import AlarmProtocol.Zone.*

  val keypadSteps: Seq[KeypadStep] = Seq(
    KeypadStep(1.seconds, Keypad.TypeDigit('1')),
    KeypadStep(2.seconds, Keypad.TypeDigit('2')),
    KeypadStep(3.seconds, Keypad.TypeDigit('3')),
    KeypadStep(4.seconds, Keypad.Enter),
    KeypadStep(6.seconds, Keypad.TypeDigit('1')),
    KeypadStep(7.seconds, Keypad.TypeDigit('2')),
    KeypadStep(8.seconds, Keypad.TypeDigit('3')),
    KeypadStep(9.seconds, Keypad.Enter),
    KeypadStep(16.seconds, Keypad.TypeDigit('1')),
    KeypadStep(17.seconds, Keypad.TypeDigit('2')),
    KeypadStep(18.seconds, Keypad.TypeDigit('3')),
    KeypadStep(19.seconds, Keypad.Enter),
    KeypadStep(20.seconds, Keypad.SelectZone(LivingRoom)),
    KeypadStep(20.seconds, Keypad.SelectZone(Kitchen)),
    KeypadStep(21.seconds, Keypad.Enter),
    KeypadStep(22.seconds, Keypad.TypeDigit('1')),
    KeypadStep(23.seconds, Keypad.TypeDigit('2')),
    KeypadStep(24.seconds, Keypad.TypeDigit('3')),
    KeypadStep(25.seconds, Keypad.Enter),
    KeypadStep(27.seconds, Keypad.TypeDigit('1')),
    KeypadStep(28.seconds, Keypad.Enter),
    KeypadStep(37.seconds, Keypad.TypeDigit('1')),
    KeypadStep(38.seconds, Keypad.TypeDigit('2')),
    KeypadStep(39.seconds, Keypad.TypeDigit('3')),
    KeypadStep(40.seconds, Keypad.Enter),
    KeypadStep(41.seconds, Keypad.TypeDigit('1')),
    KeypadStep(42.seconds, Keypad.TypeDigit('2')),
    KeypadStep(43.seconds, Keypad.TypeDigit('3')),
    KeypadStep(44.seconds, Keypad.Enter),
  )
