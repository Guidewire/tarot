package scala.guidewire.data

import scala.language.implicitConversions

import org.joda.time._

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

