package com.guidewire.tarot

import akka.actor._

import scala.language.postfixOps

import spray.util.SprayActorLogging
import spray.routing.RequestContext
import spray.http._
import spray.can.Http
import spray.json._

import ch.qos.logback.classic.spi.ILoggingEvent

class RESTLogActor(ctx: RequestContext) extends Actor with LogbackAppender.IListener with SprayActorLogging {

  implicit val execution_context = context.dispatcher

  case object Start
  case class LogEntry(level:Int, levelAsString:String, message:String)
  case class NewLogMessage(entry:LogEntry)
  case object Stop

  object Serializer extends DefaultJsonProtocol {
    implicit val log_entry_format = jsonFormat3(LogEntry)
  }

  import Serializer._

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

  def event(e: ILoggingEvent) = {
    self ! NewLogMessage(LogEntry(e.getLevel.toInt, e.getLevel.toString, e.getFormattedMessage))
  }

  def receive = {
    case Start =>
      LogbackAppender.addListener(this)

    case Stop =>
      LogbackAppender.removeListener(this)
      context.stop(self)

    case NewLogMessage(entry) =>
      send("log", entry.toJson.compactPrint)

    case ev: Http.ConnectionClosed =>
      log.warning(s"HTTP client connection closed due to: $ev")
      self ! Stop
  }
}
