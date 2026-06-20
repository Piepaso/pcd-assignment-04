package pcd.cluster.smartHomeAlarmCluster

import org.apache.pekko.actor.typed.*
import org.apache.pekko.actor.typed.scaladsl.*
import org.apache.pekko.cluster.typed.{ClusterSingleton, SingletonActor}
import com.typesafe.config.ConfigFactory
import AlarmProtocol.*

import scala.concurrent.duration.*
import scala.util.Random

object App:

  private val correctPin = "123"
  private val sensorsPositions = Map(
    "motion1" -> Zone.Kitchen,
    "motion2" -> Zone.LivingRoom,
    "window1" -> Zone.Bedroom
  )

  def rootBehavior(role: String): Behavior[Nothing] =
    Behaviors.setup[Nothing]: ctx =>

      val singleton = ClusterSingleton(ctx.system)
      val controller: ActorRef[Command] = singleton.init(
        SingletonActor(new AlarmController(correctPin, sensorsPositions).apply(), "AlarmController")
      )

      role match
        case "sensor" =>
          val id = sys.env.getOrElse("SENSOR_ID", "sensor")
          val period = ((Random.nextDouble() + 1) * 3).seconds
          val _ = ctx.spawn(Sensor(id, period), id)

        case "keypad" =>
          val _ = ctx.spawn(Keypad(controller), "keypad")

        case "control" => ()

        case other =>
          ctx.log.warn("Unknown NODE_ROLE '{}': node hosts only cluster membership.", other)

      Behaviors.empty

@main def spawnAlarmNode(): Unit =
  val host = sys.env.getOrElse("CLUSTER_IP", "127.0.0.1")
  val port = sys.env.get("CLUSTER_PORT").flatMap(_.toIntOption).getOrElse(2551)
  val seedHost = sys.env.getOrElse("SEED_PORT_1600_TCP_ADDR", host)
  val role = sys.env.getOrElse("NODE_ROLE", "control")

  val config = ConfigFactory.parseString(s"""
    pekko.remote.artery.canonical.hostname = "$host"
    pekko.remote.artery.canonical.port = $port
    pekko.remote.artery.bind.hostname = "0.0.0.0"
    pekko.remote.artery.bind.port = $port
    pekko.cluster.seed-nodes = ["pekko://ClusterSystem@$seedHost:1600"]
    """).withFallback(ConfigFactory.load("application-alarm.conf"))

  println(s"Starting Smart Home Alarm cluster node [role=$role host=$host port=$port seed=$seedHost:1600] ...")
  val _ = ActorSystem[Nothing](App.rootBehavior(role), "ClusterSystem", config)
