package com.guidewire.tarot

import scala.language.implicitConversions

import org.joda.time.{ReadableDuration, ReadablePeriod, ReadableDateTime}
import org.joda.time.{Duration, Seconds, Minutes, Hours, Days, Weeks}

package object chart {
  implicit val ReadableDateTimeOrdering = common.ReadableDateTimeOrdering
  implicit val DateTimeOrdering = common.DateTimeOrdering

  implicit def period2Interval(p:ReadablePeriod):Interval[Double] =
    duration2Interval(p.toPeriod.toStandardDuration)

  implicit def duration2Interval(d: ReadableDuration):Interval[Double] =
    Interval(d.getMillis)

  implicit class IntInterval(val underlying: Int) extends AnyVal {
    @inline def milliseconds:Interval[Double]  = Duration.millis(underlying)
    @inline def seconds:Interval[Double]       = Seconds.seconds(underlying).toStandardDuration
    @inline def minutes:Interval[Double]       = Minutes.minutes(underlying).toStandardDuration
    @inline def hours:Interval[Double]         = Hours.hours(underlying).toStandardDuration
    @inline def days:Interval[Double]          = Days.days(underlying).toStandardDuration
    @inline def weeks:Interval[Double]         = Weeks.weeks(underlying).toStandardDuration
  }

  implicit class LongInterval(val underlying: Long) extends AnyVal {
    @inline def milliseconds:Interval[Double]  = Duration.millis(underlying)
    @inline def seconds:Interval[Double]       = Seconds.seconds(underlying.toInt).toStandardDuration
    @inline def minutes:Interval[Double]       = Minutes.minutes(underlying.toInt).toStandardDuration
    @inline def hours:Interval[Double]         = Hours.hours(underlying.toInt).toStandardDuration
    @inline def days:Interval[Double]          = Days.days(underlying.toInt).toStandardDuration
    @inline def weeks:Interval[Double]         = Weeks.weeks(underlying.toInt).toStandardDuration
  }

  implicit val ReadableDateTime2LongIntervalConverter = new IntervalConverter[ReadableDateTime, Long] {
    def convert[A, B](a: A):B = a.asInstanceOf[ReadableDateTime].getMillis.asInstanceOf[B]
    def convertBackwards[B, A](b: B):A = new org.joda.time.DateTime(b.asInstanceOf[Long]).asInstanceOf[A]
  }

  implicit val ReadableDateTime2DoubleIntervalConverter = new IntervalConverter[ReadableDateTime, Double] {
    def convert[A, B](a: A):B = a.asInstanceOf[ReadableDateTime].getMillis.toDouble.asInstanceOf[B]
    def convertBackwards[B, A](b: B):A = new org.joda.time.DateTime(b.asInstanceOf[Double].toLong).asInstanceOf[A]
  }

  implicit val IdentityReadableDateTimeIntervalConverter = new IntervalConverter[ReadableDateTime, ReadableDateTime] {
    def convert[A, B](a: A):B = a.asInstanceOf[ReadableDateTime].asInstanceOf[B]
    def convertBackwards[B, A](b: B):A = b.asInstanceOf[ReadableDateTime].asInstanceOf[A]
  }

  implicit val IdentityDoubleIntervalConverter = new IntervalConverter[Double, Double] {
    def convert[A, B](a: A):B = a.asInstanceOf[Double].asInstanceOf[B]
    def convertBackwards[B, A](b: B):A = b.asInstanceOf[Double].asInstanceOf[A]
  }

  implicit val IdentityLongIntervalConverter = new IntervalConverter[Long, Long] {
    def convert[A, B](a: A):B = a.asInstanceOf[Long].asInstanceOf[B]
    def convertBackwards[B, A](b: B):A = b.asInstanceOf[Long].asInstanceOf[A]
  }

  implicit val IdentityIntIntervalConverter = new IntervalConverter[Int, Int] {
    def convert[A, B](a: A):B = a.asInstanceOf[Int].asInstanceOf[B]
    def convertBackwards[B, A](b: B):A = b.asInstanceOf[Int].asInstanceOf[A]
  }
}
