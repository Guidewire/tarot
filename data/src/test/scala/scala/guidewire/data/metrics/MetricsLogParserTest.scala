package scala.guidewire.data.metrics

import org.scalatest.junit.JUnitRunner
import org.scalatest.{SeveredStackTraces, FunSuite}
import org.junit.runner.RunWith
import org.scalatest.matchers.ShouldMatchers

import scala.guidewire.data._
import scala.guidewire.datetime._


@RunWith(classOf[JUnitRunner])
class MetricsLogParserTest extends FunSuite
                     with ShouldMatchers
                     with SeveredStackTraces {

  test("Basic metrics log parser works") {
    import ExampleMetricsLogs._

    val log = MetricsLogParser(FIRST_METRICS_LOG_FILE_URI)
    val pairs = log.producePairs()
    pairs.size should be === 485

    val hist = Histogram(15.minutes)(pairs:_*)
    hist.pairs.size should be === 9

    //TODO: Investigate why the tuple is not evaluating equality correctly. Perhaps it's doing an identity comparison?
    hist.pairs.head.toString should be === (DateTime("2013-06-21T0:51:02.597-07:00") -> 336.0).toString
  }
}
