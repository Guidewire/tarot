package com.guidewire.tarot

import akka.actor._

import scala.language.postfixOps

import spray.util.SprayActorLogging
import spray.routing.RequestContext
import spray.http._
import spray.can.Http
import spray.json._
import DefaultJsonProtocol._

import scala.concurrent.duration._

class RESTEngineActor(ctx: RequestContext, dataFeedProvider: ActorRef, engine: ActorRef) extends Actor with SprayActorLogging {
  import Engine._
  import EngineActor._
  import DataFeedActor._
  import DataFeedActor.Serialization._

  implicit val execution_context = context.dispatcher

  case object Start
  case object Tick
  case object Stop

  def json(pairs:(String, JsValue)*) = {
    val quote = "\"" //https://issues.scala-lang.org/browse/SI-6476
    val sb = new StringBuilder(128)
    sb += '{'
    for {
      (name, value) <- pairs
    }
      sb ++= s"${quote}$name${quote}: ${value.compactPrint}"
    sb += '}'
    sb.toString()
  }

  def send(category:String, data:String) = {
    val quote = "\"" //https://issues.scala-lang.org/browse/SI-6476
    ctx.responder ! MessageChunk(s"data:{${quote}$category${quote}:$data}\n\n")
  }

  ctx.responder ! ChunkedResponseStart(HttpResponse(
    entity = ":" + (" " * 2049) + "\n" //2k padding for IE
  )).withAck(Start)

  def receive = {
    case Start =>
      log.info(s"Starting HTTP client actor")
      engine ! RegisterProgressListener(self)
      engine ! RegisterFinishedListener(self)
      engine ! RegisterNewBestListener(self)

      dataFeedProvider ! RegisterDataFeedListener(self)

      context.system.scheduler.scheduleOnce(0 seconds, self, Tick)

    case Stop =>
      log.info(s"Stopping HTTP client actor")
      engine ! UnregisterProgressListener(self)
      engine ! UnregisterFinishedListener(self)
      engine ! UnregisterNewBestListener(self)

      dataFeedProvider ! UnregisterDataFeedListener(self)

      context.stop(self)

    case Tick =>
      log.info(s"Request the current results")
      engine ! RequestResult
      context.system.scheduler.scheduleOnce(1 seconds, self, Tick)

    case SuiteQueueEvent(queue) =>
      log.info(s"Suite queue: " + queue)
      send("queue", queue.toJson.compactPrint)

    case MachineEvent(machines) =>
      log.info(s"Machine information: " + machines)
      send("machines", machines.toJson.compactPrint)

    case ProgressEvent(progress) =>
      log.info(s"Progress: $progress")
      send("progress", json("done" -> progress.percentageDone.toJson))

    case NewBestResultsEvent(results) =>
      log.info(s"New best results: $results")

    case FinishedEvent(results) =>
      log.info(s"Engine pass completed: $results")

    case ResultsResponse(results) =>
      log.info(s"Results: $results")
      //ctx.responder ! MessageChunk(results.decision.toString)

    case ev: Http.ConnectionClosed =>
      log.warning(s"HTTP client connection closed due to: $ev")
      self ! Stop
  }
}
