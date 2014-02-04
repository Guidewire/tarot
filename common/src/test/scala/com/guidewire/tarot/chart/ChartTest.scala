package com.guidewire.tarot.chart

import org.scalatest.junit.JUnitRunner
import org.scalatest.{SeveredStackTraces, FunSuite}
import org.junit.runner.RunWith
import org.scalatest.matchers.ShouldMatchers
import com.guidewire.tarot.common.DateTime
import com.guidewire.tarot.metrics.{ExampleMetricsLogs, MetricsLogParser}

@RunWith(classOf[JUnitRunner])
class ChartTest extends FunSuite
                     with ShouldMatchers
                     with SeveredStackTraces {

  private[this] val logs = Seq(
      "tarot-queue-2013-6-21.log"
    , "tarot-queue-2013-6-25.log"
    , "tarot-queue-2013-6-26.log"
    , "tarot-queue-2013-6-27.log"
    , "tarot-queue-2013-6-28.log"
    , "tarot-queue-2013-6-29.log"
    , "tarot-queue-2013-6-30.log"
    , "tarot-queue-2013-7-1.log"
  )

  private[this] val winter_2007_2008 = TimeSeries("Winter 2007-2008")(
    DateTime(1970, 10, 27) -> 0.0,
    DateTime(1970, 11, 10) -> 0.6,
    DateTime(1970, 11, 18) -> 0.7,
    DateTime(1970, 12,  2) -> 0.8,
    DateTime(1970, 12,  9) -> 0.6,
    DateTime(1970, 12, 16) -> 0.6,
    DateTime(1970, 12, 28) -> 0.67,
    DateTime(1971,  1,  1) -> 0.81,
    DateTime(1971,  1,  8) -> 0.78,
    DateTime(1971,  1, 12) -> 0.98,
    DateTime(1971,  1, 27) -> 1.84,
    DateTime(1971,  2, 10) -> 1.80,
    DateTime(1971,  2, 18) -> 1.80,
    DateTime(1971,  2, 24) -> 1.92,
    DateTime(1971,  3,  4) -> 2.49,
    DateTime(1971,  3, 11) -> 2.79,
    DateTime(1971,  3, 15) -> 2.73,
    DateTime(1971,  3, 25) -> 2.61,
    DateTime(1971,  4,  2) -> 2.76,
    DateTime(1971,  4,  6) -> 2.82,
    DateTime(1971,  4, 13) -> 2.8,
    DateTime(1971,  5,  3) -> 2.1,
    DateTime(1971,  5, 26) -> 1.1,
    DateTime(1971,  6,  9) -> 0.25,
    DateTime(1971,  6, 12) -> 0.0
  )

  test("Interval implicit conversions work") {
    2.weeks           should be === Interval(1000.0D * 60 * 60 * 24 * 7 * 2)
    5.days            should be === Interval(1000.0D * 60 * 60 * 24 * 5)
    10.hours          should be === Interval(1000.0D * 60 * 60 * 10)
    30.minutes        should be === Interval(1000.0D * 60 * 30)
    90.seconds        should be === Interval(1000.0D * 90)
    2000.milliseconds should be === Interval(1000.0D * 2)
  }

  test("Creating a histogram series works") {
    import ExampleMetricsLogs._

    val log = MetricsLogParser(FIRST_METRICS_LOG_FILE_URI)
    val metrics = log.producePairs()

    val histogram_series = HistogramSeries(FIRST_METRICS_LOG_FILE_NAME, 15.minutes)(metrics:_*)

    //
  }
}
