package com.guidewire.tarot.metrics

import org.scalatest.junit.JUnitRunner
import org.scalatest.{SeveredStackTraces, FunSuite}
import org.junit.runner.RunWith
import org.scalatest.matchers.ShouldMatchers

import com.guidewire.tarot.chart._
import com.guidewire.tarot.common.DateTime

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
    hist.pairs.head.toString should be === (DateTime("2013-06-21T02:51:02.597-07:00") -> 77.0).toString
  }
}
