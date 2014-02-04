package com.guidewire.tarot.sim

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers._
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith

import scala.collection.mutable

import org.joda.time.{DateTime, Duration, Interval}
import com.guidewire.tarot.{
  Action,
  Config,
  MachineKind,
  MachineState,
  SuiteKind,
  SuiteState,
  Tracker,
  UID
}
import com.guidewire.tarot.util.UIDGenerator

@RunWith(classOf[JUnitRunner])
class SimulationStepViewTest extends FunSuite
                  with ShouldMatchers {
  private[this]
  case class TestUID(i: Int)

  private[this]
  class TestUIDGenerator extends UIDGenerator {
    private[this] var i = 0
    def apply(): UID[_] = UID{i += 1; TestUID(i)}
  }

  private[this]
  class TestSuiteGenerator(config: Config, suiteKindUID: UID[_])
    extends SuiteGenerator {
    private[this]
    val gap: Long = (1.0 / config.averageSuiteArrivalRate).round

    def apply(interval: Interval): Seq[(DateTime, UID[_])] = {
      for (timeMillis <- interval.getStartMillis() until
                         interval.getEndMillis() by
                         gap)
      yield (new DateTime(timeMillis), suiteKindUID)
    }
  }

  private[this]
  class VoidSuiteGenerator extends SuiteGenerator {
    def apply(interval: Interval) = Seq[(DateTime, UID[_])]()
  }

  test("simple suite generation") {
    val tracker = Tracker()
    val config = new Config(
      Map(),
      Map(UID("suite-kind-0") -> SuiteKind(new Duration(50L), 1)),
      simulationResolution=new Duration(100L),
      averageSuiteArrivalRate=1.0/25.0,
      randomSeed=12345L
    )
    SimulationStepView(
      tracker,
      Action(Map()),
      new DateTime(0L),
      new TestUIDGenerator,
      new TestSuiteGenerator(config, UID("suite-kind-0")),
      config
    ) should equal(Action(Map()))

    tracker.machine.list should equal (Set())
    tracker.suite.list should equal (
      Set(UID(TestUID(1)), UID(TestUID(2)), UID(TestUID(3)), UID(TestUID(4)))
    )

    for (suite <- tracker.suite.list.map(tracker.suite.get(_).get)) {
      suite.kindUID should equal (UID("suite-kind-0"))
      suite.life.size should equal (1)
      suite.life(0).time compareTo new DateTime(0L) should be >= (0)
      suite.life(0).time compareTo new DateTime(100L) should be < (0)
      suite.life(0).value should be (SuiteState.QUEUED)
    }
  }

  test("machines should only pick up queued suites") {
    val config = new Config(
      Map(UID("machine-kind-0") ->
          MachineKind(new Duration(10L), new Duration(10L), 0, 1.0)
      ),
      Map(UID("suite-kind-0") -> SuiteKind(new Duration(50L), 0)),
      simulationResolution=new Duration(100L),
      averageSuiteArrivalRate=0.0,
      randomSeed=12345L
    )

    val tracker = Tracker()

    tracker.machine.add(UID("machine-0"), UID("machine-kind-0"))
    tracker.machine.update(
      UID("machine-0"),
      new DateTime(-100L),
      MachineState.ACTIVE
    )

    tracker.suite.add(UID("suite-0A"), UID("suite-kind-0"))
    tracker.suite.update(
      UID("suite-0A"),
      new DateTime(-10L),
      SuiteState.QUEUED
    )

    tracker.suite.add(UID("suite-0B"), UID("suite-kind-0"))
    tracker.suite.update(
      UID("suite-0B"),
      new DateTime(-70L),
      SuiteState.QUEUED
    )

    tracker.suite.add(UID("suite-0C"), UID("suite-kind-0"))
    tracker.suite.update(
      UID("suite-0C"),
      new DateTime(-100L),
      SuiteState.INACTIVE
    )

    SimulationStepView(
      tracker,
      Action(Map()),
      new DateTime(0L),
      new TestUIDGenerator,
      new VoidSuiteGenerator,
      config
    ) should be (Action(Map()))

    tracker.machine.list should be (Set(UID("machine-0")))

    {
      val x = tracker.machine.get(UID("machine-0")).get.life
      x.size should be (1)
      x(0).time should be (new DateTime(-100L))
      x(0).value should be (MachineState.ACTIVE)
    }

    tracker.suite.list should equal (
      Set(UID("suite-0A"), UID("suite-0B"), UID("suite-0C"))
    )

    {
      val x = tracker.suite.get(UID("suite-0A")).get.life
      x.size should be (3)
      x(0).time should be (new DateTime(-10L))
      x(0).value should be (SuiteState.QUEUED)
      x(1).time should be (new DateTime(0L))
      x(1).value should be (SuiteState.RUNNING(UID("machine-0")))
      x(2).time should be (new DateTime(50L))
      x(2).value should be (SuiteState.INACTIVE)
    }

    {
      val x = tracker.suite.get(UID("suite-0B")).get.life
      x.size should be (3)
      x(0).time should be (new DateTime(-70L))
      x(0).value should be (SuiteState.QUEUED)
      x(1).time should be (new DateTime(0L))
      x(1).value should be (SuiteState.RUNNING(UID("machine-0")))
      x(2).time should be (new DateTime(50L))
      x(2).value should be (SuiteState.INACTIVE)
    }

    {
      val x = tracker.suite.get(UID("suite-0C")).get.life
      x.size should be (1)
      x(0).time should be (new DateTime(-100L))
      x(0).value should be (SuiteState.INACTIVE)
    }
  }

  test("machines should not run more than their capacity") {
    val config = new Config(
      Map(UID("machine-kind-0") ->
          MachineKind(new Duration(10L), new Duration(10L), 1, 1.0)
      ),
      Map(UID("suite-kind-0") -> SuiteKind(new Duration(50L), 1)),
      simulationResolution=new Duration(100L),
      averageSuiteArrivalRate=0.0,
      randomSeed=12345L
    )

    val tracker = Tracker()

    tracker.machine.add(UID("machine-0"), UID("machine-kind-0"))
    tracker.machine.update(
      UID("machine-0"),
      new DateTime(-100L),
      MachineState.ACTIVE
    )

    tracker.suite.add(UID("suite-0A"), UID("suite-kind-0"))
    tracker.suite.update(
      UID("suite-0A"),
      new DateTime(-10L),
      SuiteState.QUEUED
    )

    tracker.suite.add(UID("suite-0B"), UID("suite-kind-0"))
    tracker.suite.update(
      UID("suite-0B"),
      new DateTime(-70L),
      SuiteState.QUEUED
    )

    def checkMachine(): Unit = {
      tracker.machine.list should be (Set(UID("machine-0")))
      val x = tracker.machine.get(UID("machine-0")).get.life
      x.size should be (1)
      x(0).time should be (new DateTime(-100L))
      x(0).value should be (MachineState.ACTIVE)
    }

    def checkSuites(): Unit = {
      tracker.suite.list should equal (
        Set(UID("suite-0A"), UID("suite-0B"))
      )

      {
        val x = tracker.suite.get(UID("suite-0A")).get.life
        x(0).time should be (new DateTime(-10L))
        x(0).value should be (SuiteState.QUEUED)
      }

      {
        val x = tracker.suite.get(UID("suite-0B")).get.life
        x.size should be (3)
        x(0).time should be (new DateTime(-70L))
        x(0).value should be (SuiteState.QUEUED)
        x(1).time should be (new DateTime(0L))
        x(1).value should be (SuiteState.RUNNING(UID("machine-0")))
        x(2).time should be (new DateTime(50L))
        x(2).value should be (SuiteState.INACTIVE)
      }
    }

    SimulationStepView(
      tracker,
      Action(Map()),
      new DateTime(0L),
      new TestUIDGenerator,
      new VoidSuiteGenerator,
      config
    ) should be (Action(Map()))

    checkMachine()
    checkSuites()
    tracker.suite.get(UID("suite-0A")).get.life.size should be (1)

    SimulationStepView(
      tracker,
      Action(Map()),
      new DateTime(100L),
      new TestUIDGenerator,
      new VoidSuiteGenerator,
      config
    ) should be (Action(Map()))

    checkMachine()
    checkSuites()

    {
      val x = tracker.suite.get(UID("suite-0A")).get.life
      x.size should be (3)
      x(1).time should be (new DateTime(100L))
      x(1).value should be (SuiteState.RUNNING(UID("machine-0")))
      x(2).time should be (new DateTime(150L))
      x(2).value should be (SuiteState.INACTIVE)
    }
  }
}
