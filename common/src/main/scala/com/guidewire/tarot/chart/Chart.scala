package com.guidewire.tarot.chart

import scala.language.implicitConversions

import org.joda.time._
import com.guidewire.tarot.common.StringUtil

trait Series[X,Y] {
  def title:String
  def pairs:Seq[(X, Y)]
}

trait Interval[X] {
  def size:X
  def ordering:Ordering[X]
}

trait IntervalConverter[A, B] {
  def convert[A2 >: A, B2 >: B](a:A2):B2
  def convertBackwards[B2 >: B, A2 >: A](b:B2):A2
}

object Interval {
  private[this] case class StandardInterval[X](size:X, ordering:Ordering[X]) extends Interval[X]

  implicit def interval2Ordered[X](i:Interval[X]):Ordered[Interval[X]] = new Ordered[Interval[X]] {
    def compare(that: Interval[X]) = i.ordering.compare(i.size, that.size)
  }

  def apply:Interval[Long] = apply(1L)
  def apply(inter:Duration):Interval[Double] = apply(inter)
  def apply[X](inter:X)(implicit ordering:Ordering[X]):Interval[X] = StandardInterval(inter, ordering)
}

trait Histogram[X] {
  def pairs:Seq[(X, Double)]
  def interval:Interval[Double]
  def included[T >: X](start:T, end:T, value:T)(implicit ordering:Ordering[T]):Boolean =
    ordering.compare(start, value) <= 0 && ordering.compare(end, value) > 0
}

object Histogram {
  private case class StandardHistogram[X, Y](interval:Interval[Double], rawPoints:Seq[(X, Y)], toDouble:Y => Double, ordering:Ordering[X], converter:IntervalConverter[X, Double]) extends Histogram[X] {
    private[this] def init():Seq[(X, Double)] = {
      var first:Option[X] = None
      var last:Option[X] = None

      def check(point:(X, Y), current:Option[X], lt:Boolean):Option[X] = {
        val (point_bucket_part, _) = point

        if (current.isEmpty) {
          Some(point_bucket_part)
        } else {
          val current_bucket_part = current.get

          if (lt && ordering.lt(point_bucket_part, current_bucket_part) || !lt && ordering.gteq(point_bucket_part, current_bucket_part))
            Some(point_bucket_part)
          else
            Some(current_bucket_part)
        }
      }

      rawPoints.foreach { point =>
        first = check(point, first, lt = true)
        last = check(point, last, lt = false)
      }

      //Generate bins
      val start = converter.convert[X, Double](first.get)
      val end = converter.convert[X, Double](last.get)

      val pairs = for {
        i <- start to end by interval.size
        j = i + interval.size

        starting = converter.convertBackwards[Double, X](i)
        ending = converter.convertBackwards[Double, X](j)

        includedPoints = rawPoints.filter(p => included(starting, ending, p._1)(ordering))
        includedValues = includedPoints.map(p => toDouble(p._2))
        sum = includedValues.sum
      } yield starting -> sum

      pairs
    }

    val pairs = init()
  }

  def apply[TBucket, TValue](interval:Interval[Double])(rawPoints:(TBucket, TValue)*)(implicit toDouble:TValue => Double, ordering:Ordering[TBucket], converter:IntervalConverter[TBucket, Double]):Histogram[TBucket] =
    new StandardHistogram(interval, rawPoints, toDouble, ordering, converter)
}

object HistogramSeries {
  private[this] case class HistogramSeries[X](title:String, histogram:Histogram[X]) extends Series[X, Double] {
    def pairs = histogram.pairs
  }

  def apply[TBucket, TValue](title:String)(histogram: Histogram[TBucket]):Series[TBucket, Double] =
    HistogramSeries(title, histogram)

  def apply[TBucket, TValue](title:String, interval:Interval[Double])(rawPoints:(TBucket, TValue)*)(implicit toDouble:TValue => Double, ordering:Ordering[TBucket], converter:IntervalConverter[TBucket, Double]):Series[TBucket, Double] =
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