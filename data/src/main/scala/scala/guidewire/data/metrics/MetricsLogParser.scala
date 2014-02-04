package scala.guidewire.data.metrics

import org.joda.time.ReadableDateTime
import scala.io.BufferedSource
import java.net.{URL, URI}
import java.io.File

case class LogEntry(name:String, queueDuration:Long, runDuration:Long, enteredQueue:ReadableDateTime, leftQueue:ReadableDateTime, runFinished:ReadableDateTime)

trait MetricsLog {
  def entries:Seq[LogEntry]

  protected def produceEntries(source:BufferedSource):Seq[LogEntry] = {
    def str2Date(str:String):ReadableDateTime =
      org.joda.time.DateTime.parse(str)

    //Explicitly NOT toSeq because we need every entry read out for now or it would produce a Seq that reads the entries
    //on-demand and since we call source.close() later on in order to clean things up, it would leave a reference
    //to a now-closed file descriptor. Easily solvable in the future by requesting a BufferedSource in
    //MetricsLogParser.apply().

    source.getLines.map { l =>
      val arr = l.split(",")
      LogEntry(arr(0), arr(2).toLong, arr(4).toLong, str2Date(arr(6)), str2Date(arr(8)), str2Date(arr(10)))
    }.toList
  }

  def producePairs():Seq[(ReadableDateTime, Double)] =
    for {
      e <- entries
      s = e.enteredQueue
    } yield s -> 1.0D
}

object MetricsLogParser {
  private[this] class StandardMetricsLog(source:BufferedSource) extends MetricsLog {
    val entries = produceEntries(source)
  }

  def apply(file:String):MetricsLog =
    apply(new File(file).toURI)

  def apply(file:File):MetricsLog =
    apply(file.toURI)

  def apply(url:URL):MetricsLog =
    apply(url.toURI)

  def apply(uri:URI):MetricsLog = {
    val source = scala.io.Source.fromFile(uri, "utf-8")
    val log = new StandardMetricsLog(source)
    source.close()

    log
  }
}
