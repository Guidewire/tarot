package com.guidewire.tarot

import akka.actor._

import scala.language.postfixOps

import spray.util.SprayActorLogging
import spray.routing.RequestContext
import spray.http._
import spray.can.Http
import spray.json._

class RESTConfigurationActor(ctx: RequestContext, engine: ActorRef) extends Actor with SprayActorLogging {
  import EngineActor._

  implicit val execution_context = context.dispatcher

  case object Start
  case object Tick
  case object Stop

  case class SuiteKindConfiguration(id:String, title:String)
  case class MachineKindConfiguration(id:String, title:String, compatibleSuiteKinds:Iterable[String])
  case class EngineConfiguration(machineKinds:Iterable[MachineKindConfiguration], suiteKinds:Iterable[SuiteKindConfiguration])
  case class RESTConfigurationResponse(engine:EngineConfiguration)

  object Serialization extends DefaultJsonProtocol {
    implicit val suite_kind_format = jsonFormat2(SuiteKindConfiguration)
    implicit val machine_kind_format = jsonFormat3(MachineKindConfiguration)
    implicit val engine_format = jsonFormat2(EngineConfiguration)
    implicit val configuration_format = jsonFormat1(RESTConfigurationResponse)

    /**
     * Converts a configuration into a serializable JSON object.
     */
    def configurationResponseForConfig(config:Config):JsValue = {
      val machine_kinds =
        for {
          (uid, kind) <- config.machineKinds
          id = uid.friendlyValue
          title = uid.value.toString
          compatibleSuiteKinds = config.suiteKindsFor(uid).map(_.friendlyValue)
        } yield MachineKindConfiguration(id, title, compatibleSuiteKinds)

      val suite_kinds =
        for {
          (uid, kind) <- config.suiteKinds
          id = uid.friendlyValue
          title = uid.value.toString
        } yield SuiteKindConfiguration(id, title)

      RESTConfigurationResponse(EngineConfiguration(machine_kinds, suite_kinds)).toJson
    }
  }

  import Serialization._

  ctx.responder ! ChunkedResponseStart(HttpResponse(
    entity = (" " * 2049) + "\n" //2k padding for IE
  )).withAck(Start)

  def receive = {
    case Start =>
      log.info(s"Responding to request for configuration")
      engine ! RequestConfiguration

    case Stop =>
      log.info(s"Stopping request for configuration")
      context.stop(self)

    case ConfigurationResponse(config) =>
      log.info(s"Configuration: $config")
      ctx.responder ! MessageChunk(configurationResponseForConfig(config).prettyPrint)
      ctx.responder ! ChunkedMessageEnd
      self ! Stop

    case ev: Http.ConnectionClosed =>
      log.warning(s"HTTP client connection closed due to: $ev")
      self ! Stop
  }
}
