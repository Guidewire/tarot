package com.guidewire.tarot.sa

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers._
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith

import org.joda.time.{DateTime, Duration}
import com.guidewire.tarot.{
  Action,
  Config,
  MachineKind,
  MachineState,
  SampleHappyFunc,
  SuiteKind,
  Tracker,
  UID
}

@RunWith(classOf[JUnitRunner])
class SimulatedAnnealingTest extends FunSuite
                             with ShouldMatchers {
  private[this]
  val saConfig = SimulatedAnnealingConfig(
    simulationDepth=30,
    suiteWaitPrice=25.0,
    SAEvaluateRepeats=1,
    SAMutateDensity=5,
    SAMutateSize=1,
    SAMutateProbability=1.0,
    SATemperatureCutoff=0.02,
    SATemperatureDecay=0.97,
    notifyProgressListeners=_=>Unit
  )

  private[this]
  val machineKinds = Map[UID[_], MachineKind](
    UID("Machine Type A") -> MachineKind(
      startDuration=new Duration(1000L),
      stopDuration=new Duration(1000L),
      capacity=2,
      runCost=1.0
    )
  )

  private[this]
  val suiteKinds = Map[UID[_], SuiteKind](
    UID("Suite Type A") -> SuiteKind(
      runDuration=new Duration(1000L * 3L),
      capacity=1
    )
  )

  private[this]
  def makeConfig(suiteRate: Double) = new Config(
    machineKinds=machineKinds,
    suiteKinds=suiteKinds,
    simulationResolution=new Duration(1000L),
    averageSuiteArrivalRate=suiteRate,
    randomSeed=12345L
  )


  test("should not modify tracker") {
    val config = makeConfig(1e-2)
    val tracker = Tracker()
    SimulatedAnnealing(
      tracker,
      new DateTime(0),
      new SampleHappyFunc(config, saConfig),
      config,
      saConfig
    )
    tracker.suite.list should be (Set())
    tracker.machine.list should be (Set())
  }

  test("should spin up more machines in response to more incoming suites") {
    def getDeltaSum(config: Config): Int = {
      val decisions = SimulatedAnnealing(
        Tracker(),
        new DateTime(0),
        new SampleHappyFunc(config, saConfig),
        config,
        saConfig
      )
      val deltas: Seq[Int] =
        for (action <- decisions)
        yield action.delta(UID("Machine Type A"))

      deltas.sum
    }

    val d1 = getDeltaSum(makeConfig(1e-3))
    val d2 = getDeltaSum(makeConfig(1e-2))
    d1 should be > (0)
    d2 should be > (d1)
  }

  test("should not do anything when tracker is empty") {
    val config = makeConfig(0.0)
    val decisions = SimulatedAnnealing(
      Tracker(),
      new DateTime(0),
      new SampleHappyFunc(config, saConfig),
      config,
      saConfig
    )

    for (action <- decisions) {
      action.delta(UID("Machine Type A")) should equal (0)
    }
  }

  test("should shut down all machines when queue is empty") {
    val config = makeConfig(0.0)

    val tracker = Tracker()
    tracker.machine.add(UID("m0"), UID("Machine Type A"))
    tracker.machine.add(UID("m1"), UID("Machine Type A"))
    tracker.machine.add(UID("m2"), UID("Machine Type A"))
    tracker.machine.update(UID("m0"), new DateTime(-10L), MachineState.ACTIVE)
    tracker.machine.update(UID("m1"), new DateTime(-10L), MachineState.ACTIVE)
    tracker.machine.update(UID("m2"), new DateTime(-10L), MachineState.ACTIVE)

    val decisions: Seq[Action] = SimulatedAnnealing(
      tracker,
      new DateTime(0L),
      new SampleHappyFunc(config, saConfig),
      config,
      saConfig
    )

    for (action <- decisions) {
      val d = action.delta(UID("Machine Type A"))
      d should be <= (0)
      d >= (-3)
    }

    decisions.map(_.delta(UID("Machine Type A"))).sum should equal (-3)
  }

  test("don't blow up with zero machine kinds and suite kinds") {
    val config = new Config(
      machineKinds=Map(),
      suiteKinds=Map(),
      simulationResolution=new Duration(1000L),
      averageSuiteArrivalRate=1.0,
      randomSeed=12345L
    )

    val decisions: Seq[Action] = SimulatedAnnealing(
      Tracker(),
      new DateTime(0L),
      new SampleHappyFunc(config, saConfig),
      config,
      saConfig
    )

    decisions.size should equal (saConfig.simulationDepth)
    for (action <- decisions) {
      action.delta should equal (Map())
    }
  }
}
