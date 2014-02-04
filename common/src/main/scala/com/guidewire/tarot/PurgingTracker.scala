package com.guidewire.tarot

import scala.concurrent.duration._

/**
 * Associates data with metadata.
 *
 * @tparam A Type of data to track.
 * @tparam B Any associated metadata with respect to consumers.
 */
trait TrackedData[+A, B] {
  def datum:A
  def associated:B
}

/**
 * A strategy defining how to remove and then add to.
 *
 * @tparam A Type of data that will be purged.
 */
trait PurgingStrategy[+A] {
  def purge[B >: A](existing: Iterable[B], additional: Iterable[B]): Iterable[B]
}

trait Tracking[A, B, C <: TrackedData[A, B]] {
  def data:Iterable[C]
}

private[tarot] object BoundedMaximum {
  trait TrackerBoundedMaximum {
    def maximum:Int
  }

  sealed case class BoundedMaximumData[A](val datum:A) extends TrackedData[A, Unit] {
    val associated = {}
  }

  sealed trait BoundedMaximumPurgingStrategy[A] extends PurgingStrategy[BoundedMaximumData[A]] {
    this:TrackerBoundedMaximum =>

    def purge[B >: BoundedMaximumData[A]](existing: Iterable[B], additional: Iterable[B]) = {
      import math._

      val amount_from_addl = min(additional.size, maximum)
      val amount_from_existing = max(existing.size - min(existing.size, existing.size + additional.size - maximum), 0)

      val existing_allowed = existing.takeRight(amount_from_existing)
      val additional_allowed = additional.takeRight(amount_from_addl)

      existing_allowed ++ additional_allowed
    }
  }

  sealed class BoundedMaximumPurgingTracker[A](val maximum:Int, val data:Iterable[BoundedMaximumData[A]])
    extends PurgingTracker[A, Unit, BoundedMaximumData[A], Unit]
    with TrackerBoundedMaximum
    with Tracking[A, Unit, BoundedMaximumData[A]]
    with BoundedMaximumPurgingStrategy[A] {

    require(maximum >= 0, s"maximum must be greater than or equal to zero")

    def copy(data: Iterable[BoundedMaximumData[A]]) =
      new BoundedMaximumPurgingTracker[A](maximum, data)

    def mapWithMetadata[X >: A](values: Iterable[X]) =
      values.map(v => BoundedMaximumData(v.asInstanceOf[A]))

    def lookup[X >: Unit](value: X):Option[A] = ??? //Not applicable here.
  }
}

private[tarot] object Timed {
  val DEFAULT_WINDOW = 1.second

  trait TrackerTiming {
    def trackingWindow:Long
  }

  sealed class TimedData[A](val datum:A, val time:Long = System.nanoTime()) extends (Long, A)(time, datum) with TrackedData[A, Long] {
    val associated = time
  }

  sealed trait TimedPurgingStrategy[A] extends PurgingStrategy[TimedData[A]] { this: TrackerTiming =>
    def purge[B >: TimedData[A]](existing: Iterable[B], additional: Iterable[B]) = {
      val last = System.nanoTime() - trackingWindow

      val existing_with_room_for_additional =
        for {
          e <- existing if e.asInstanceOf[TimedData[A]].time >= last
        }
        yield e

      (existing_with_room_for_additional ++ additional).toSeq.sortBy(_.asInstanceOf[TimedData[A]].time)
    }
  }

  sealed class TimedPurgingTracker[A](val trackingWindow:Long, val data:Iterable[TimedData[A]])
    extends PurgingTracker[A, Long, TimedData[A], Long]
    with TrackerTiming
    with Tracking[A, Long, TimedData[A]]
    with TimedPurgingStrategy[A] {

    def copy(data: Iterable[TimedData[A]]) =
      new TimedPurgingTracker[A](trackingWindow, data)

    def mapWithMetadata[X >: A](values: Iterable[X]) =
      values.map(v => new TimedData(v.asInstanceOf[A]))

    def lookup[X >: Long](value: X):Option[A] = {
      //We know it's a sequence b/c our purging strategy only generates sorted seqs.
      val seq_data = data.asInstanceOf[Seq[TimedData[A]]]
      val window_start = value.asInstanceOf[Long] - trackingWindow // ((value.asInstanceOf[Long] / trackingWindow) + 1) * trackingWindow
      val window_end = window_start + trackingWindow + trackingWindow

      var left = 0
      var right = seq_data.size - 1
      var selected:Option[A] = None

      //Binary search (O(log n)) for data that fits in this window.
      while(left <= right && selected.isEmpty) {
        val mid = left + (right - left) / 2
        val value_at_mid = seq_data(mid)
        val time_at_mid = value_at_mid.time

        if (time_at_mid < window_start) {
          left = mid + 1
        } else if (time_at_mid > window_end) {
          right = mid - 1
        } else {
          selected = Some(value_at_mid.datum)
        }
      }

      selected
    }
  }
}

/**
 * Represents something that employs a regular eviction policy such as
 * only holding data within a specified time window or only the last X
 * number of values.
 *
 * @tparam A The type of data this instance contains.
 */
trait TrackerLike[A, TLookup] extends Iterable[A] {
  def lookup[X >: TLookup](value:X):Option[A]

  def apply[X >: TLookup](value:X):Option[A] =
    lookup(value)

  def +[X >: A](additional:X):TrackerLike[A, TLookup] =
    append[X](Seq(additional))

  def ++[X >: A](additional:Iterable[X]):TrackerLike[A, TLookup] =
    append[X](additional)

  def append[X >: A](additional:X):TrackerLike[A, TLookup] =
    append(Seq(additional))

  def append[X >: A](additional:Iterable[X]):TrackerLike[A, TLookup]
}

trait PurgingTracker[A, B, C <: TrackedData[A, B], TLookup] extends TrackerLike[A, TLookup] {
  this:PurgingStrategy[C] with Tracking[A, B, C] =>

  def lookup[X >: TLookup](value:X):Option[A]
  def copy(data:Iterable[C]):PurgingTracker[A, B, C, TLookup]
  def mapWithMetadata[X >: A](values:Iterable[X]):Iterable[C]

  def append[X >: A](additional:Iterable[X]):TrackerLike[A, TLookup] =
    copy(purge(data, mapWithMetadata(additional)))

  override def size:Int = data.size

  def iterator = {
    val iter = data.iterator
    new Iterator[A] {
      def hasNext = iter.hasNext
      def next() = iter.next().datum
    }
  }
}

object PurgingTracker {
  import Timed._
  import BoundedMaximum._

  def withTimingWindow[A](window:Duration = DEFAULT_WINDOW):TrackerLike[A, Long] =
    new TimedPurgingTracker[A](window.toNanos, Seq())

  def withMaximumSize[A](maximum:Int):TrackerLike[A, Unit] =
    new BoundedMaximumPurgingTracker[A](maximum, Seq())
}
