package scala.guidewire.collections

import scala.concurrent.duration._

/**
 * Associates data with metadata.
 *
 * @tparam TDatum Type of data to track.
 * @tparam TAssociated Any associated metadata with respect to consumers.
 */
trait EvictionSlot[+TDatum, TAssociated] {
  def datum:TDatum
  def associated:TAssociated
}

trait EvictionStrategySlotMagnet

/**
 * A strategy defining how to remove and then add to.
 *
 * @tparam TEvictionSlot Type of data that will be purged.
 */
trait EvictionStrategy[TEvictionSlot, TMagnet <: EvictionStrategySlotMagnet] {
  def evictAndAppend[A <: TEvictionSlot, B <: TMagnet](existing: Iterable[A], additional: Iterable[A])(implicit magnet:B): Iterable[A]
}

trait Evicting[TDatum, TAssociated, TEvictionSlot <: EvictionSlot[TDatum, TAssociated]] {
  def data:Iterable[TEvictionSlot]
}

private[collections] object BoundedMaximum {
  trait AutoEvictionBoundedMaximum {
    def maximum:Int
  }

  trait EmptyData extends EvictionStrategySlotMagnet

  implicit val NoDataMagnet = new EmptyData { }

  sealed case class BoundedMaximumEvictionSlot[TDatum](val datum:TDatum) extends EvictionSlot[TDatum, Unit] {
    val associated = {}
  }

  sealed trait BoundedMaximumEvictionStrategy[TDatum] extends EvictionStrategy[BoundedMaximumEvictionSlot[TDatum], EmptyData] {
    this:AutoEvictionBoundedMaximum =>

    def evictAndAppend[A <: BoundedMaximumEvictionSlot[TDatum], B <: EmptyData](existing: Iterable[A], additional: Iterable[A])(implicit magnet:B) = {
      import math._

      val amount_from_addl = min(additional.size, maximum)
      val amount_from_existing = max(existing.size - min(existing.size, existing.size + additional.size - maximum), 0)

      val existing_allowed = existing.takeRight(amount_from_existing)
      val additional_allowed = additional.takeRight(amount_from_addl)

      existing_allowed ++ additional_allowed
    }
  }

  sealed class BoundedMaximumAutoEvictionList[TDatum](val maximum:Int, val data:Iterable[BoundedMaximumEvictionSlot[TDatum]])
    extends AutoEvictionList[TDatum, Unit, BoundedMaximumEvictionSlot[TDatum], Unit, EmptyData]
    with AutoEvictionBoundedMaximum
    with Evicting[TDatum, Unit, BoundedMaximumEvictionSlot[TDatum]]
    with BoundedMaximumEvictionStrategy[TDatum] {

    require(maximum >= 0, s"maximum must be greater than or equal to zero")

    def copy(data: Iterable[BoundedMaximumEvictionSlot[TDatum]]) =
      new BoundedMaximumAutoEvictionList[TDatum](maximum, data)

    def mapWithMetadata[A <: TDatum](values: Iterable[A]) =
      values.map(v => BoundedMaximumEvictionSlot(v.asInstanceOf[TDatum]))

    def lookup[A <: Unit](value: A):Option[TDatum] = ??? //Not applicable here.
  }
}

private[collections] object Timed {
  val DEFAULT_WINDOW = 1.second

  trait TrackerTiming {
    def trackingWindow:Long
  }

  trait Clock extends EvictionStrategySlotMagnet {
    def foo = println("foo")
  }

  implicit val SystemMonotonicClock = new Clock {
    override def foo = println("system clock")
  }

  sealed class TimedEvictionSlot[TDatum](val datum:TDatum, val time:Long = System.nanoTime()) extends (Long, TDatum)(time, datum) with EvictionSlot[TDatum, Long] {
    val associated = time
  }

  sealed trait TimedEvictionStrategy[TDatum] extends EvictionStrategy[TimedEvictionSlot[TDatum], Clock] { this: TrackerTiming =>
    def evictAndAppend[A <: TimedEvictionSlot[TDatum], B <: Clock](existing: Iterable[A], additional: Iterable[A])(implicit clock:B) = {
      val last = System.nanoTime() - trackingWindow

      val existing_with_room_for_additional =
        for {
          e <- existing if e.time >= last
        }
        yield e

      (existing_with_room_for_additional ++ additional).toSeq.sortBy(_.asInstanceOf[TimedEvictionSlot[TDatum]].time)
    }
  }

  sealed class TimedAutoEvictionList[TDatum](val trackingWindow:Long, val data:Iterable[TimedEvictionSlot[TDatum]])
    extends AutoEvictionList[TDatum, Long, TimedEvictionSlot[TDatum], Long, Clock]
    with TrackerTiming
    with Evicting[TDatum, Long, TimedEvictionSlot[TDatum]]
    with TimedEvictionStrategy[TDatum] {

    def copy(data: Iterable[TimedEvictionSlot[TDatum]]) =
      new TimedAutoEvictionList[TDatum](trackingWindow, data)

    def mapWithMetadata[A <: TDatum](values: Iterable[A]) =
      values.map(v => new TimedEvictionSlot(v.asInstanceOf[TDatum]))

    def lookup[A <: Long](value: A):Option[TDatum] = {
      //We know it's a sequence b/c our purging strategy only generates sorted seqs.
      val seq_data = data.asInstanceOf[Seq[TimedEvictionSlot[TDatum]]]
      val window_start = value - trackingWindow // ((value.asInstanceOf[Long] / trackingWindow) + 1) * trackingWindow
      val window_end = window_start + trackingWindow + trackingWindow

      var left = 0
      var right = seq_data.size - 1
      var selected:Option[TDatum] = None

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
 * @tparam TDatum The type of data this instance contains.
 */
trait AutoEvictionLike[TDatum, TLookup, TMagnet] extends Iterable[TDatum] {
  def lookup[A <: TLookup](value:A):Option[TDatum]

  def apply[A <: TLookup](value:A):Option[TDatum] =
    lookup(value)

  def +[A <: TDatum, B <: TMagnet](additional:A)(implicit magnet:B):AutoEvictionLike[TDatum, TLookup, TMagnet] =
    append[A, B](Seq(additional))(magnet)

  def ++[A <: TDatum, B <: TMagnet](additional:Iterable[A])(implicit magnet:B):AutoEvictionLike[TDatum, TLookup, TMagnet] =
    append[A, B](additional)(magnet)

  def append[A <: TDatum, B <: TMagnet](additional:A)(implicit magnet:B):AutoEvictionLike[TDatum, TLookup, TMagnet] =
    append[A, B](Seq(additional))(magnet)

  def append[A <: TDatum, B <: TMagnet](additional:Iterable[A])(implicit magnet:B):AutoEvictionLike[TDatum, TLookup, TMagnet]
}

trait AutoEvictionList[TDatum, TAssociated, TTrackedData <: EvictionSlot[TDatum, TAssociated], TLookup, TMagnet <: EvictionStrategySlotMagnet] extends AutoEvictionLike[TDatum, TLookup, TMagnet] {
  this:EvictionStrategy[TTrackedData, TMagnet] with Evicting[TDatum, TAssociated, TTrackedData] =>

  def copy(data:Iterable[TTrackedData]):AutoEvictionList[TDatum, TAssociated, TTrackedData, TLookup, TMagnet]
  def mapWithMetadata[A <: TDatum](values:Iterable[A]):Iterable[TTrackedData]

  def append[A <: TDatum, B <: TMagnet](additional:Iterable[A])(implicit magnet:B):AutoEvictionLike[TDatum, TLookup, TMagnet] =
    copy(evictAndAppend(data, mapWithMetadata(additional))(magnet))

  override def size:Int = data.size

  def iterator = {
    val iter = data.iterator
    new Iterator[TDatum] {
      def hasNext = iter.hasNext
      def next() = iter.next().datum
    }
  }
}

object AutoEvictionList {
  import Timed._
  import BoundedMaximum._

  def withTimingWindow[A](window:Duration = DEFAULT_WINDOW):AutoEvictionLike[A, Long, Clock] =
    new TimedAutoEvictionList[A](window.toNanos, Seq())

  def withMaximumSize[A](maximum:Int):AutoEvictionLike[A, Unit, EmptyData] =
    new BoundedMaximumAutoEvictionList[A](maximum, Seq())
}
