package com.guidewire.tarot.timeseries

import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, BeforeAndAfter}
import org.scalatest.matchers.ShouldMatchers._
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith

import org.joda.time.{DateTime, Duration, Interval}

@RunWith(classOf[JUnitRunner])
class TimeSeriesTest extends FunSuite
                     with ShouldMatchers {
  test("find() on empty series") {
    val t = TimeSeries[String]()

    t.find(new DateTime(100L)) should be (None)
    t.find(new DateTime(1000L)) should be (None)
  }

  test("find() on series with one point") {
    val t = TimeSeries[String]()
            .add(new DateTime(200L), "spam")

    t.find(new DateTime(100L)) should be (None)
    t.find(new DateTime(199L)) should be (None)

    t.find(new DateTime(200L)) should be (Some(0))
    t.find(new DateTime(201L)) should be (Some(0))
    t.find(new DateTime(1000L)) should be (Some(0))
  }

  test("find() on typical series") {
    val t = TimeSeries[String]()
            .add(new DateTime(200L), "Hi")
            .add(new DateTime(300L), "How are you?")
            .add(new DateTime(400L), "Bye")

    t.find(new DateTime(100L)) should be (None)
    t.find(new DateTime(199L)) should be (None)

    t.find(new DateTime(200L)) should be (Some(0))

    t.find(new DateTime(300L)) should be (Some(1))
    t.find(new DateTime(350L)) should be (Some(1))
    t.find(new DateTime(399L)) should be (Some(1))

    t.find(new DateTime(400L)) should be (Some(2))
    t.find(new DateTime(401L)) should be (Some(2))
    t.find(new DateTime(11401L)) should be (Some(2))
  }

  test("findPoint()") {
    val t = TimeSeries[String]()
            .add(new DateTime(200L), "Hi")
            .add(new DateTime(300L), "How are you?")
            .add(new DateTime(400L), "Bye")
    t.findPoint(new DateTime(200L)).get.value should be ("Hi")
    t.findPoint(new DateTime(300L)).get.value should be ("How are you?")
    t.findPoint(new DateTime(400L)).get.value should be ("Bye")

    t.findPoint(new DateTime(100L)) should be (None)
  }

  test("countTime() on empty series") {
    val t = TimeSeries[String]()
    t.countTime((v: String) => true,
                new Interval(100L, 500L)) should be (new Duration(0L))
    t.countTime((v: String) => true,
                new Interval(100L, 100L)) should be (new Duration(0L))
  }

  test("countTime() on series with one point") {
    val t = TimeSeries[String]()
            .add(new DateTime(200L), "Hi")
    def x(v: String) = true

    t.countTime(x, new Interval(100L, 150L)) should be (new Duration(0L))
    t.countTime(x, new Interval(100L, 200L)) should be (new Duration(0L))

    t.countTime(x, new Interval(198L, 201L)) should be (new Duration(1L))
    t.countTime(x, new Interval(200L, 201L)) should be (new Duration(1L))

    t.countTime(x, new Interval(170L, 370L)) should be (new Duration(170L))

    t.countTime(x, new Interval(300L, 500L)) should be (new Duration(200L))
    t.countTime(x, new Interval(200L, 500L)) should be (new Duration(300L))
    t.countTime(x, new Interval(201L, 500L)) should be (new Duration(299L))
  }

  test("countTime() on typical series") {
    val t = TimeSeries[String]()
            .add(new DateTime(200L), "Hi")
            .add(new DateTime(300L), "How are you?")
            .add(new DateTime(400L), "Bye")
    def x(v: String) = v.size < 5

    t.countTime((v: String) => v == "How are you?",
                new Interval(0L, 10000L)) should be (new Duration(100L))

    t.countTime(x, new Interval(250L, 700L)) should be (new Duration(350L))
    t.countTime(x, new Interval(300L, 1000L)) should be (new Duration(600L))
    t.countTime(x, new Interval(300L, 399L)) should be (new Duration(0L))
    t.countTime(x, new Interval(300L, 400L)) should be (new Duration(0L))
    t.countTime(x, new Interval(300L, 401L)) should be (new Duration(1L))
    t.countTime(x, new Interval(299L, 401L)) should be (new Duration(2L))
  }

  test("disallow inserting earlier points") {
    val t = TimeSeries[String]().add(new DateTime(200L), "Hi")
    try {
      t.add(new DateTime(100L), "Bye")
      fail
    } catch {
      case e: AssertionError =>
    }
    try {
      t.add(new DateTime(200L), "Bye")
      fail
    } catch {
      case e: AssertionError =>
    }
    t.find(new DateTime(200L)) should equal (Some(0))
    t.find(new DateTime(199L)) should equal (None)
  }

  test("pruning history should leave only one point") {
    val t = TimeSeries[String]()
            .add(new DateTime(200L), "now")
            .add(new DateTime(400L), "you")
            .add(new DateTime(500L), "see")
            .add(new DateTime(600L), "me")
            .add(new DateTime(700L), "now")
            .add(new DateTime(800L), "YOU")
            .add(new DateTime(801L), "dont")
            .prune()
    t.size should be (1)
    t(0).time should equal (new DateTime(801L))
    t(0).value should equal ("dont")
  }
}
