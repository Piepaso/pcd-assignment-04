package pcd.cluster.smartHomeAlarm

object AlarmProtocol:

  enum Zone:
    case LivingRoom
    case Kitchen
    case Bedroom
    case Perimeter
    
  trait Command
  trait PhysicalEvent
  
  enum Message extends Command:
    case PinEntered(pin: String)
    case SelectZones(zones: Set[Zone])
    case SensorTriggered(zone: Zone)

  enum Timeout extends Command:
    case ExitTimeout
    case EntryTimeout
    
  export Zone.*, Message.*, Timeout.*