package scala.guidewire.collections

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith

import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class AutoEvictionListTest extends FunSuite
                  with ShouldMatchers {

  test("Basic timed eviction works as expected") {
    val m1 = AutoEvictionList.withTimingWindow[Int](50.milliseconds)
    val m2 = m1 + 1

    //If we haven't added anything, then there's no head.
    m1.headOption should be (None)

    //Adding 1 item should give us a head value.
    m2.head should be (1)

    //Test that we can get a value right now.
    m2(System.nanoTime()) should be (Some(1))

    //Wait a period of time well enough past our period for
    //which data should be purged when add'l values are added.
    Thread.sleep(100.milliseconds.toMillis)

    //We should no longer be able to find this value b/c we've
    //slept for a while and asking for the new time should place
    //us beyond the threshold to locate the data.
    m2(System.nanoTime()) should be (None)

    val m3 = m2 append 2 append 3

    //NOT three b/c after sleeping, the addition of another value should cause the first to be purged.
    m3.size should be (2)
    m3.toSeq should be (Seq(2, 3))
  }

  test("Max size eviction should throw exception if given negative size") {
    intercept[IllegalArgumentException] {
      AutoEvictionList.withMaximumSize(-1)
    }
  }

  test("Basic max size eviction works as expected") {
    val p1 = AutoEvictionList.withMaximumSize[Int](0)
    (p1 + 0).toSeq should be ('empty)
    (p1 append 0 append 1).toSeq should be ('empty)

    val p2 = AutoEvictionList.withMaximumSize[Int](1)
    (p2 + 0).toSeq should be (Seq(0))
    (p2 append 1 append 2).toSeq should be (Seq(2))

    val p3 = AutoEvictionList.withMaximumSize[Int](3)
    (p3 append 0 append 1).toSeq should be (Seq(0, 1))
    (p3 append 0 append 1 append 2).toSeq should be (Seq(0, 1, 2))
    (p3 append 0 append 1 append 2 append 3).toSeq should be (Seq(1, 2, 3))
    (p3 append Seq(0, 1, 2)).toSeq should be (Seq(0, 1, 2))
    (p3 append Seq(0, 1, 2, 3)).toSeq should be (Seq(1, 2, 3))
    (p3 append Seq(0, 1, 2, 3, 4, 5, 6)).toSeq should be (Seq(4, 5, 6))
    (p3 append Seq(0) append Seq(1, 2)).toSeq should be (Seq(0, 1, 2))
    (p3 append Seq(0) append Seq(1, 2, 3)).toSeq should be (Seq(1, 2, 3))
    (p3 append Seq(0, 1) append Seq(2, 3)).toSeq should be (Seq(1, 2, 3))
    (p3 append Seq(0, 1) append Seq(1, 2, 3)).toSeq should be (Seq(1, 2, 3))
    (p3 append Seq(0) append Seq(1, 2, 3, 4)).toSeq should be (Seq(2, 3, 4))
    (p3 append Seq(0, 1) append Seq(1, 2, 3, 4)).toSeq should be (Seq(2, 3, 4))

    val p4 = AutoEvictionList.withMaximumSize[Int](10)
    (p4 ++ (0 to 50)).toList should be (Seq(41, 42, 43, 44, 45, 46, 47, 48, 49, 50))
  }

}
