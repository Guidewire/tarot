package com.guidewire.tarot

import scala.concurrent.duration._

/**
 * Associates data with metadata.
 *
 * @tparam TData Type of data to track.
 * @tparam TAssociated Any associated metadata with respect to consumers.
 */
trait TrackedData[+TData, TAssociated] {
  def datum:TData
  def associated:TAssociated
}

trait PurgingStrategyDataMagnet

/**
 * A strategy defining how to remove and then add to.
 *
 * @tparam TData Type of data that will be purged.
 */
trait PurgingStrategy[TData, TMagnet <: PurgingStrategyDataMagnet] {
  def purge[A <: TData, B <: TMagnet](existing: Iterable[A], additional: Iterable[A])(implicit magnet:B): Iterable[A]
}

trait Tracking[TData, TAssociated, TTrackedData <: TrackedData[TData, TAssociated]] {
  def data:Iterable[TTrackedData]
}

private[tarot] object BoundedMaximum {
  trait TrackerBoundedMaximum {
    def maximum:Int
  }

  trait EmptyData extends PurgingStrategyDataMagnet

  implicit val NoDataMagnet = new EmptyData { }

  sealed case class BoundedMaximumData[TData](val datum:TData) extends TrackedData[TData, Unit] {
    val associated = {}
  }

  sealed trait BoundedMaximumPurgingStrategy[TData] extends PurgingStrategy[BoundedMaximumData[TData], EmptyData] {
    this:TrackerBoundedMaximum =>

    def purge[A <: BoundedMaximumData[TData], B <: EmptyData](existing: Iterable[A], additional: Iterable[A])(implicit magnet:B) = {
      import math._

      val amount_from_addl = min(additional.size, maximum)
      val amount_from_existing = max(existing.size - min(existing.size, existing.size + additional.size - maximum), 0)

      val existing_allowed = existing.takeRight(amount_from_existing)
      val additional_allowed = additional.takeRight(amount_from_addl)

      existing_allowed ++ additional_allowed
    }
  }

  sealed class BoundedMaximumPurgingTracker[TData](val maximum:Int, val data:Iterable[BoundedMaximumData[TData]])
    extends PurgingTracker[TData, Unit, BoundedMaximumData[TData], Unit, EmptyData]
    with TrackerBoundedMaximum
    with Tracking[TData, Unit, BoundedMaximumData[TData]]
    with BoundedMaximumPurgingStrategy[TData] {

    require(maximum >= 0, s"maximum must be greater than or equal to zero")

    def copy(data: Iterable[BoundedMaximumData[TData]]) =
      new BoundedMaximumPurgingTracker[TData](maximum, data)

    def mapWithMetadata[A <: TData](values: Iterable[A]) =
      values.map(v => BoundedMaximumData(v.asInstanceOf[TData]))

    def lookup[A <: Unit](value: A):Option[TData] = ??? //Not applicable here.
  }
}

private[tarot] object Timed {
  val DEFAULT_WINDOW = 1.second

  trait TrackerTiming {
    def trackingWindow:Long
  }

  trait Clock extends PurgingStrategyDataMagnet {
    def foo = println("foo")
  }

  implicit val SystemMonotonicClock = new Clock {
    override def foo = println("system clock")
  }

  sealed class TimedData[TData](val datum:TData, val time:Long = System.nanoTime()) extends (Long, TData)(time, datum) with TrackedData[TData, Long] {
    val associated = time
  }

  sealed trait TimedPurgingStrategy[TData] extends PurgingStrategy[TimedData[TData], Clock] { this: TrackerTiming =>
    def purge[A <: TimedData[TData], B <: Clock](existing: Iterable[A], additional: Iterable[A])(implicit clock:B) = {
      val last = System.nanoTime() - trackingWindow

      val existing_with_room_for_additional =
        for {
          e <- existing if e.time >= last
        }
        yield e

      (existing_with_room_for_additional ++ additional).toSeq.sortBy(_.asInstanceOf[TimedData[TData]].time)
    }
  }

  sealed class TimedPurgingTracker[TData](val trackingWindow:Long, val data:Iterable[TimedData[TData]])
    extends PurgingTracker[TData, Long, TimedData[TData], Long, Clock]
    with TrackerTiming
    with Tracking[TData, Long, TimedData[TData]]
    with TimedPurgingStrategy[TData] {

    def copy(data: Iterable[TimedData[TData]]) =
      new TimedPurgingTracker[TData](trackingWindow, data)

    def mapWithMetadata[A <: TData](values: Iterable[A]) =
      values.map(v => new TimedData(v.asInstanceOf[TData]))

    def lookup[A <: Long](value: A):Option[TData] = {
      //We know it's a sequence b/c our purging strategy only generates sorted seqs.
      val seq_data = data.asInstanceOf[Seq[TimedData[TData]]]
      val window_start = value - trackingWindow // ((value.asInstanceOf[Long] / trackingWindow) + 1) * trackingWindow
      val window_end = window_start + trackingWindow + trackingWindow

      var left = 0
      var right = seq_data.size - 1
      var selected:Option[TData] = None

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
 * @tparam TData The type of data this instance contains.
 */
trait TrackerLike[TData, TLookup, TMagnet] extends Iterable[TData] {
  def lookup[A <: TLookup](value:A):Option[TData]

  def apply[A <: TLookup](value:A):Option[TData] =
    lookup(value)

  def +[A <: TData, B <: TMagnet](additional:A)(implicit magnet:B):TrackerLike[TData, TLookup, TMagnet] =
    append[A, B](Seq(additional))(magnet)

  def ++[A <: TData, B <: TMagnet](additional:Iterable[A])(implicit magnet:B):TrackerLike[TData, TLookup, TMagnet] =
    append[A, B](additional)(magnet)

  def append[A <: TData, B <: TMagnet](additional:A)(implicit magnet:B):TrackerLike[TData, TLookup, TMagnet] =
    append[A, B](Seq(additional))(magnet)

  def append[A <: TData, B <: TMagnet](additional:Iterable[A])(implicit magnet:B):TrackerLike[TData, TLookup, TMagnet]
}

trait PurgingTracker[TData, TAssociated, TTrackedData <: TrackedData[TData, TAssociated], TLookup, TMagnet <: PurgingStrategyDataMagnet] extends TrackerLike[TData, TLookup, TMagnet] {
  this:PurgingStrategy[TTrackedData, TMagnet] with Tracking[TData, TAssociated, TTrackedData] =>

  def copy(data:Iterable[TTrackedData]):PurgingTracker[TData, TAssociated, TTrackedData, TLookup, TMagnet]
  def mapWithMetadata[A <: TData](values:Iterable[A]):Iterable[TTrackedData]

  def append[A <: TData, B <: TMagnet](additional:Iterable[A])(implicit magnet:B):TrackerLike[TData, TLookup, TMagnet] =
    copy(purge(data, mapWithMetadata(additional))(magnet))

  override def size:Int = data.size

  def iterator = {
    val iter = data.iterator
    new Iterator[TData] {
      def hasNext = iter.hasNext
      def next() = iter.next().datum
    }
  }
}

object PurgingTracker {
  import Timed._
  import BoundedMaximum._

  def withTimingWindow[A](window:Duration = DEFAULT_WINDOW):TrackerLike[A, Long, Clock] =
    new TimedPurgingTracker[A](window.toNanos, Seq())

  def withMaximumSize[A](maximum:Int):TrackerLike[A, Unit, EmptyData] =
    new BoundedMaximumPurgingTracker[A](maximum, Seq())
}
