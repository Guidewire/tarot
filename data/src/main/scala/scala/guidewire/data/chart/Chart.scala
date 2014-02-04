package scala.guidewire.data.chart

import scala.language.implicitConversions

import org.joda.time._

import scala.guidewire.data._
import scala.guidewire.core.StringUtil

trait Series[X,Y] {
  def title:String
  def pairs:Seq[(X, Y)]
}

object HistogramSeries {
  private[this] case class HistogramSeries[X](title:String, histogram:Histogram[X]) extends Series[X, Double] {
    def pairs = histogram.pairs
  }

  def apply[TBucket, TValue](title:String)(histogram: Histogram[TBucket]):Series[TBucket, Double] =
    HistogramSeries(title, histogram)

  def apply[TBucket, TValue](title:String, interval:guidewire.data.Interval[Double])(rawPoints:(TBucket, TValue)*)(implicit toDouble:TValue => Double, ordering:Ordering[TBucket], converter:IntervalConverter[TBucket, Double]):Series[TBucket, Double] =
    HistogramSeries(title, Histogram(interval)(rawPoints:_*)(toDouble, ordering, converter))
}

object TimeSeries {
  private[this] case class TimeSeries[X <: ReadableDateTime, Y](title:String, pairs:Seq[(X, Y)]) extends Series[X, Y]

  def apply[TTime <: ReadableDateTime, TValue](title:String)(pairs:(TTime, TValue)*): Series[TTime, TValue] =
    TimeSeries(title, pairs)
}

object ValueSeries {
  private[this] case class ValueSeries[X <: Double, Y <: Double](title:String, pairs:Seq[(X, Y)]) extends Series[X, Y]

  def apply[X <: Double, Y <: Double](title:String)(pairs:(X, Y)*): Series[X, Y] =
    ValueSeries(title, pairs)
}

trait Chart[X, Y] {
  type ChartSeries = Series[X, Y]

  def title:String
  def subtitle:String
  def series:Seq[ChartSeries]
  def ++(newSeries: Series[X, Y]):Chart[X, Y]
}

object Chart {
  private[this] case class StandardChart[X, Y](title:String, subtitle:String, series:Seq[Series[X, Y]]) extends Chart[X, Y] {
    def ++(newSeries: Series[X, Y]):Chart[X, Y] = copy(series = series :+ newSeries)
  }

  def apply[X, Y](title:String, subtitle:String = StringUtil.empty)(series: Series[X, Y]*): Chart[X, Y] = StandardChart(title, subtitle, series)
}