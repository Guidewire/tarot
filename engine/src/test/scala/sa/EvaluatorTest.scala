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
  SampleHappyFunc,
  SuiteKind,
  SuiteState,
  Tracker,
  UID
}

@RunWith(classOf[JUnitRunner])
class EvaluatorTest extends FunSuite
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
    new Evaluator(
      tracker,
      new DateTime(0L),
      new SampleHappyFunc(config, saConfig),
      config,
      saConfig
    )(
      for (_ <- 0 until saConfig.simulationDepth)
      yield Action(Map(UID("Machine Type A") -> 2))
    )

    tracker.suite.list should equal (Set())
    tracker.machine.list should equal (Set())
  }

  test("idle running machines should be less happy") {
    val config = makeConfig(0.0)
    val evaluator = new Evaluator(
      Tracker(),
      new DateTime(0L),
      new SampleHappyFunc(config, saConfig),
      config,
      saConfig
    )
    val (happiness1, _) = evaluator(
      for (_ <- 0 until saConfig.simulationDepth)
      yield Action(Map(UID("Machine Type A") -> 2))
    )

    val (happiness2, _) = evaluator(
      for (_ <- 0 until saConfig.simulationDepth)
      yield Action(Map())
    )

    happiness1 should be < (happiness2)
  }

  test("finishing suites should be more happy") {
    val config = makeConfig(0.0)
    val tracker = Tracker()
    tracker.suite.add(UID("s0"), UID("Suite Type A"))
    tracker.suite.add(UID("s1"), UID("Suite Type A"))
    tracker.suite.add(UID("s2"), UID("Suite Type A"))
    tracker.suite.update(UID("s0"), new DateTime(-10L), SuiteState.QUEUED)
    tracker.suite.update(UID("s1"), new DateTime(-10L), SuiteState.QUEUED)
    tracker.suite.update(UID("s2"), new DateTime(-10L), SuiteState.QUEUED)

    val (happiness1, _) = new Evaluator(
      tracker,
      new DateTime(0L),
      new SampleHappyFunc(config, saConfig),
      config,
      saConfig
    )(
      for (_ <- 0 until saConfig.simulationDepth)
      yield Action(Map(UID("Machine Type A") -> 1))
    )

    val (happiness2, _) = new Evaluator(
      tracker,
      new DateTime(0L),
      new SampleHappyFunc(config, saConfig),
      config,
      saConfig
    )(
      for (_ <- 0 until saConfig.simulationDepth)
      yield Action(Map())
    )

    happiness1 should be > (happiness2)
  }
}
