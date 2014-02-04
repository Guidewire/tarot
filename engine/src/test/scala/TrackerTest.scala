package com.guidewire.tarot

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers._
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith

import org.joda.time.DateTime

@RunWith(classOf[JUnitRunner])
class TrackerTest extends FunSuite
                  with ShouldMatchers {
  test("clone()") {
    val orig = Tracker()
    orig.machine.add(UID("orig A"), UID("machine kind"))
    orig.machine.add(UID("orig B"), UID("machine kind"))
    orig.suite.add(UID("orig X"), UID("suite kind"))
    orig.machine.update(UID("orig A"),
                        new DateTime(100L),
                        MachineState.INACTIVE)

    val clone = orig.clone()

    clone.machine.add(UID("new A"), UID("machine kind"))
    clone.suite.add(UID("new X"), UID("suite kind"))
    clone.machine.update(UID("orig A"),
                         new DateTime(500L),
                         MachineState.POWERING_ON)

    clone.machine.get(UID("orig B")) should equal (
      orig.machine.get(UID("orig B"))
    )

    orig.machine.get(UID("new A"))  should equal (None)
    clone.machine.get(UID("new A")) should not equal (None)

    orig.suite.get(UID("new X"))  should equal (None)
    clone.suite.get(UID("new X")) should not equal (None)

    def machineValue(tracker: Tracker, millis: Long): MachineState.Value =
      tracker.machine.get(UID("orig A")).get
             .life.findPoint(new DateTime(millis)).get.value

    machineValue(orig, 200L) should equal (MachineState.INACTIVE)
    machineValue(orig, 800L) should equal (MachineState.INACTIVE)
    machineValue(clone, 200L) should equal (MachineState.INACTIVE)
    machineValue(clone, 800L) should equal (MachineState.POWERING_ON)
  }

  test("machine.update()") {
    val tracker = Tracker()
    tracker.machine.add(UID("my machine"), UID("the machine kind"))

    Seq((100L, MachineState.POWERING_ON),
        (150L, MachineState.ACTIVE),
        (200L, MachineState.POWERING_OFF),
        (300L, MachineState.INACTIVE)
    ) foreach (datum => {
      val (t, s) = datum
      tracker.machine.update(UID("my machine"), new DateTime(t), s)
    })

    Seq((130L, MachineState.POWERING_ON),
        (180L, MachineState.ACTIVE),
        (299L, MachineState.POWERING_OFF),
        (1000L, MachineState.INACTIVE)
    ) foreach (datum => {
      val (t, s) = datum
      val machine = tracker.machine.get(UID("my machine")).get
      machine.kindUID should equal (UID("the machine kind"))
      machine.life.findPoint(new DateTime(t)).get.value should equal (s)
    })
  }

  test("suite.update()") {
    val tracker = Tracker()
    tracker.machine.add(UID("my machine"), UID("the machine kind"))
    tracker.suite.add(UID("my suite"), UID("the suite kind"))

    Seq((100L, SuiteState.QUEUED),
        (150L, SuiteState.RUNNING(UID("my machine"))),
        (200L, SuiteState.RUNNING(UID("my machine"))),
        (300L, SuiteState.INACTIVE)
    ) foreach (datum => {
      val (t, s) = datum
      tracker.suite.update(UID("my suite"), new DateTime(t), s)
    })

    Seq((130L, SuiteState.QUEUED),
        (180L, SuiteState.RUNNING(UID("my machine"))),
        (299L, SuiteState.RUNNING(UID("my machine"))),
        (1000L, SuiteState.INACTIVE)
    ) foreach (datum => {
      val (t, s) = datum
      val suite = tracker.suite.get(UID("my suite")).get
      suite.kindUID should equal (UID("the suite kind"))
      suite.life.findPoint(new DateTime(t)).get.value should equal (s)
    })
  }

  test("should not update() nonexistent objects") {
    val tracker = Tracker()
    tracker.suite.add(UID("me"), UID("my kind"))
    try {
      tracker.suite.update(UID("you"), new DateTime(300L), SuiteState.QUEUED)
      fail
    } catch {
      case e: NoSuchElementException =>
    }
  }

  test("should not re-add() objects with same UID") {
    val tracker = Tracker()
    def add(): Unit = tracker.suite.add(UID("tom"), UID("person"))
    add()
    try {
      add()
      fail
    } catch {
      case e: AssertionError =>
    }
  }

  test("prune() should only keep \"interesting\" objects") {
    val tracker = Tracker()

    (()=>{
      val u = (x:Any, y:Long, z:SuiteState.Value) =>
              tracker.suite.update(UID(x), new DateTime(y), z)

      tracker.suite.add(UID("suite alive"), UID("my kind"))
      u("suite alive", 0L, SuiteState.QUEUED)
      u("suite alive", 1L, SuiteState.INACTIVE)
      u("suite alive", 2L, SuiteState.QUEUED)

      tracker.suite.add(UID("suite dead"), UID("my kind"))
      u("suite dead", 0L, SuiteState.INACTIVE)
      u("suite dead", 1L, SuiteState.QUEUED)
      u("suite dead", 2L, SuiteState.INACTIVE)

      tracker.suite.add(UID("suite empty"), UID("my kind"))
    })()

    (()=>{
      val u = (x:Any, y:Long, z:MachineState.Value) =>
               tracker.machine.update(UID(x), new DateTime(y), z)

      tracker.machine.add(UID("machine alive"), UID("my kind"))
      u("machine alive", 0L, MachineState.POWERING_OFF)
      u("machine alive", 1L, MachineState.INACTIVE)
      u("machine alive", 2L, MachineState.POWERING_ON)

      tracker.machine.add(UID("machine dead"), UID("my kind"))
      u("machine dead", 0L, MachineState.INACTIVE)
      u("machine dead", 1L, MachineState.ACTIVE)
      u("machine dead", 2L, MachineState.INACTIVE)

      tracker.machine.add(UID("machine empty"), UID("my kind"))
    })()

    tracker.prune()

    (()=>{
      tracker.suite.list should equal (Set(UID("suite alive")))
      val life = tracker.suite.get(UID("suite alive")).get.life
      life.size should equal (1)
      life(0).time should equal (new DateTime(2L))
      life(0).value should equal (SuiteState.QUEUED)
    })()

    (()=>{
      tracker.machine.list should equal (Set(UID("machine alive")))
      val life = tracker.machine.get(UID("machine alive")).get.life
      life.size should equal (1)
      life(0).time should equal (new DateTime(2L))
      life(0).value should equal (MachineState.POWERING_ON)
    })()
  }
}
