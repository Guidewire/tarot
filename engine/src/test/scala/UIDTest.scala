package com.guidewire.tarot

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers._
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class UIDTest extends FunSuite
              with ShouldMatchers {
  private[this]
  def chkeq(expect: Boolean, a: Any, b: Any): Unit = {
    val t1 = a == b
    val t2 = a equals b
    if ((t1 ^ expect) || (t2 ^ expect)) fail
  }

  test("UID equality") {
    chkeq(true, UID(1), UID(1))
    chkeq(false, UID(1), UID(2))

    chkeq(true, UID("foo"), UID("foo"))
    chkeq(false, UID("foo"), UID("bar"))

    chkeq(false, UID(1), UID("foo"))
    chkeq(false, UID(1), UID(UID(1)))
    chkeq(true, UID(UID(1)), UID(UID(1)))
  }

  test("UID hashing") {
    val m = Map[UID[_], Int]((UID(1) -> 700),
                             (UID("hi") -> 800),
                             (UID(UID(3)) -> 600))
    m(UID(1)) should equal(700)
    m(UID("hi")) should equal(800)
    m(UID(UID(3))) should equal(600)
    m.get(UID(4)) should equal(None)
  }

}
