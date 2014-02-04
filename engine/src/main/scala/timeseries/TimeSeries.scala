package com.guidewire.tarot.timeseries

import scala.annotation.tailrec
import org.joda.time.{DateTime, Duration, Interval}

/** Factory object */
object TimeSeries {
  /** Return a new, empty [[TimeSeries]] instance */
  def apply[V]() = new TimeSeries[V](Seq[TimeSeriesPoint[V]]())
}

private[timeseries]
sealed class BaseTimeSeries[V] protected[this]
  (points: Seq[TimeSeriesPoint[V]]) {

  /** Returns number of points in this */
  def size: Int = points.size

  /** Gets the `i`th point
    *
    * @param i Index of point to fetch
    */
  def apply(i: Int): TimeSeriesPoint[V] = points(i)

  /** Returns a new [[TimeSeries]] with new point
    *
    * @param time Time of new point
    * @param value Value of new point
    *
    * @throws IllegalArgumentException
    *         if `time` is on or before time of last point
    */
  def add(time: DateTime, value: V): TimeSeries[V] = {
    assert(points.size == 0 ||
           points(points.size - 1).time.compareTo(time) < 0,
           points.map(_.time).toString() + " is not behind " +
             time.toString())
    new TimeSeries(points :+ new TimeSeriesPoint(time, value))
  }

  /** Returns a new [[TimeSeries]] containing only the most recent point */
  def prune(): TimeSeries[V] = new TimeSeries(
    points.lastOption match {
      case None => Seq[TimeSeriesPoint[V]]()
      case Some(last) => Seq(last)
    }
  )
}

private[timeseries]
sealed trait HaveGetters[V] {
  def size: Int
  def apply(i: Int): TimeSeriesPoint[V]
}

private[timeseries]
sealed trait HaveFind[V] extends HaveGetters[V] {
  /** Returns index of most recent point with time on or before `time`,
    * or `None` if no such point exists
    *
    * @param time Points are searched on or before this moment
    */
  def find(time: DateTime): Option[Int] = {
    /* shortcut if there are no points */
    if (size == 0)
      return None

    /* shortcut if target is before 'lo' */
    if (this(0).time.compareTo(time) > 0)
      return None

    /* shortcut if target is after 'hi' */
    if (this(size - 1).time.compareTo(time) <= 0)
      return Some(size - 1)

    /* if (size < 2) then we would have already taken a shortcut */
    assert(size >= 2)

    @tailrec
    def finder(lo: Int, hi: Int): Int = {
      val mid = (lo + hi) / 2
      assert(lo <= mid)
      assert(mid < hi)
      if (this(mid).time.compareTo(time) <= 0)
        if (lo == mid) mid else finder(mid, hi)
      else
        finder(lo, mid)
    }
    Some(finder(0, size - 1))
  }
}

private[timeseries]
sealed trait HaveFindPoint[V] extends HaveFind[V] {
  /** Returns the point found by [[find]]
    *
    * [[find]] returns an index;
    * if successful, [[findPoint]] calls [[apply]]`()` on the index,
    * otherwise returns `None`.
    */
  def findPoint(time: DateTime): Option[TimeSeriesPoint[V]] =
    find(time) match {
      case None => None
      case Some(i) => Some(this(i))
    }
}

private[timeseries]
sealed trait HaveCountTime[V] extends HaveFind[V] {
  /** Counts the time that this series has certain values
    *
    * @param filter Defines the values in this series to be counted
    * @param interval Interval over which this series is examined
    */
  def countTime(filter: (V) => Boolean, interval: Interval): Duration = {
    def filterHelp(i: Int, trueExpr: () => (DateTime, DateTime)): Duration =
      if (filter(this(i).value)) {
        val args = trueExpr()
        new Duration(args._1, args._2)
      } else {
        Duration.ZERO
      }

    val endIndex: Int = find(interval.getEnd()) match {
      case None => return Duration.ZERO
      case Some(i) => i
    }

    @tailrec
    def counter(curr: Int, cumulative: Duration): Duration = {
      assert(interval.getStart().compareTo(this(curr).time) <= 0)
      if (curr + 1 <= endIndex) {
        assert(interval.getEnd().compareTo(this(curr + 1).time) >= 0)
        val more = filterHelp(curr, ()=>(this(curr).time, this(curr + 1).time))
        counter(curr + 1, cumulative.plus(more))
      } else {
        cumulative
      }
    }

    val tailDuration =
      filterHelp(endIndex, ()=>(this(endIndex).time, interval.getEnd()))

    val startIndex: Int = find(interval.getStart()) match {
      case None => return counter(0, tailDuration)
      case Some(i) => i
    }
    assert(startIndex <= endIndex)

    if (startIndex + 1 >= size) {
      assert(startIndex == size - 1 && startIndex == endIndex)
      assert(interval.getStart().compareTo(this(startIndex).time) >= 0)
      filterHelp(startIndex, ()=>(interval.getStart(), interval.getEnd()))
    } else {
      val headDur = filterHelp(startIndex, ()=>(interval.getStart(),
                                                this(startIndex + 1).time))
      counter(startIndex + 1, headDur.plus(tailDuration))
    }
  }
}

/** Tracks values over time
  *
  * @tparam V value type
  */
class TimeSeries[V] private[timeseries] (_x: Seq[TimeSeriesPoint[V]])
  extends BaseTimeSeries[V](_x) with HaveFindPoint[V] with HaveCountTime[V]
