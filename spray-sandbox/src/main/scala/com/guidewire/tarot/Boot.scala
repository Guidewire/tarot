package com.guidewire.tarot

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.io.IO
import spray.can.Http
import com.typesafe.config.ConfigFactory
import com.guidewire.tarot.SimulatorActor.StartUp
import spray.util.LoggingContext

object Boot extends App {
  val configuration = ConfigFactory.load().withFallback(ConfigFactory.parseString(
    """
      | tarot {
      |  http-server-interface = "0.0.0.0"
      |  http-server-port = 8999
      |}
      |
      |akka {
      |  loglevel = INFO
      |  loggers = ["akka.event.slf4j.Slf4jLogger"]
      |}
    """.stripMargin))

  //We need an ActorSystem to host our application in.
  implicit val system = ActorSystem("tarot-simulator")

  //Create the engine and simulator actors.
  //The simulator actor will push messages to the engine periodically.
  //We also create an actor that will respond to HTTP client requests and act
  //as a REST endpoint.
  val engine:ActorRef    = EngineActor("simulated-annealing-engine")
  val simulator:ActorRef = SimulatorActor(engine, "toolsharness-simulator")
  val service:ActorRef   = RESTActor(simulator, engine, simulator, "rest-endpoint")
  val machineProvision:ActorRef   = MachineProvisionActor(engine, "machine-provision")

  //Notify the simulator to start things up.
  simulator ! StartUp

  //Start a new HTTP server on the configured port with our REST service actor as the handler.
  IO(Http) ! Http.Bind(service, interface = configuration.getString("tarot.http-server-interface"), port = configuration.getInt("tarot.http-server-port"))
}
