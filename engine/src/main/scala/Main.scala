package com.guidewire.tarot

import scala.collection.mutable
import org.joda.time.{DateTime, Duration, Interval}

import com.guidewire.tarot.sa.{SimulatedAnnealing, SimulatedAnnealingConfig}
import com.guidewire.tarot.sim.{
  SimulationStepView,
  SuiteGenerator,
  PoissonSuiteGenerator
}
import com.guidewire.tarot.util.UIDGenerator

object Main {
  private[this]
  sealed case class WorldUID(value: Long)

  private[this]
  val config = new Config(
    machineKinds=Map(
      UID("Machine Type A") -> MachineKind(
        startDuration=new Duration(1000L),
        stopDuration=new Duration(1000L),
        capacity=1,
        runCost=1.0)
      ,
      UID("Machine Type B") -> MachineKind(
        startDuration=new Duration(1000L),
        stopDuration=new Duration(1000L),
        capacity=3,
        runCost=5.0)
    ),
    suiteKinds=Map(
      UID("Suite Type A") -> SuiteKind(
        runDuration=new Duration(1000L * 2L),
        capacity=1
      ),
      UID("Suite Type B") -> SuiteKind(
        runDuration=new Duration(1000L * 5L),
        capacity=3
      )
    ),
    simulationResolution=new Duration(1000L),
    averageSuiteArrivalRate=0.0,
    randomSeed=12345L
  )

  private[this]
  val saConfig = SimulatedAnnealingConfig(
    simulationDepth=30,
    suiteWaitPrice=25.0,
    SAEvaluateRepeats=1,
    SAMutateDensity=10,
    SAMutateSize=1,
    SAMutateProbability=1.0,
    SATemperatureCutoff=0.02,
    SATemperatureDecay=0.9,
    notifyProgressListeners=(_:Double)=>print(".")
  )

  private[this]
  val happy = new SampleHappyFunc(config, saConfig)

  private[this]
  val uidGenerator: UIDGenerator = new UIDGenerator {
    private[this] var curr = 0L
    def apply(): UID[_] = UID(WorldUID{curr += 1; curr})
  }

  private[this]
  val suiteGenerator: SuiteGenerator = new SuiteGenerator {
    private[this]
    var lastNum = 0

    private[this]
    def pickRandomKey[K, V](map: Map[K, V]): K =
      map.keySet.toIndexedSeq(config.random.nextInt(map.size))

    def apply(interval: Interval): Seq[(DateTime, UID[_])] = {
      assert(interval.toDurationMillis() >= 1L)
      val length: Double = (interval.toDurationMillis() - 1L).toDouble

      val inString = {print("> "); readLine()}
      val numSuites =
        if (inString equals "") lastNum
        else try {
          inString.toInt
        } catch {
          case e: NumberFormatException => 0
        }
      lastNum = numSuites
      for (_ <- 0 until numSuites)
      yield {
        val dur: Long = (config.random.nextDouble() * length).round
        val suiteTime = interval.getStart().plus(dur)
        val suiteKindUID = pickRandomKey(config.suiteKinds)
        (suiteTime, suiteKindUID)
      }
    }
  }

  private[this]
  def computeAction(tracker: Tracker, now: DateTime): Action = {
    val decisions = SimulatedAnnealing(tracker, now, happy, config, saConfig)
    println()
    decisions(0)
  }

  private[this]
  def displayTracker(tracker: Tracker, now: DateTime) {
    println(s"now: ${now.getMillis()}")

    println("Queue Length: " + tracker.suite.queue(now).size.toString())

    val activeMachinesCount = mutable.Map[UID[_], Int]()
    for {machine <- tracker.machine.list map (tracker.machine.get(_).get)
         nowPoint <- machine.life findPoint now
         if nowPoint.value == MachineState.ACTIVE
    } activeMachinesCount(machine.kindUID) =
        activeMachinesCount.get(machine.kindUID) match {
          case None => 1
          case Some(oldCount) =>
            assert(oldCount >= 1)
            oldCount + 1
        }
    println("Active Machines: " + activeMachinesCount.toString())

    val runningSuitesCount = mutable.Map[UID[_], Int]()
    for {suite <- tracker.suite.list map (tracker.suite.get(_).get)
         nowPoint <- suite.life findPoint now
    } nowPoint.value match {
      case SuiteState.RUNNING(parentUID) =>
        val x = tracker.machine.get(parentUID).get.kindUID
        runningSuitesCount(x) =
          runningSuitesCount get x match {
            case None => 1
            case Some(oldCount) =>
              assert(oldCount >= 1)
              oldCount + 1
          }
      case _ =>
    }
    println("Running Suites: " + runningSuitesCount.toString())
  }

  private[this]
  def displayAction(action: Action) {
    if (false) println("applied action: " + action.toString())
  }

  private[this]
  def doSim(tracker: Tracker, action: Action, now: DateTime): Action =
    SimulationStepView(
      tracker, action, now, uidGenerator, suiteGenerator, config
    )

  def main(args: Array[String]) {
    val tracker = Tracker()
    var now = new DateTime(0L)
    while (true) {
      tracker.prune()
      val action = computeAction(tracker, now)
      displayTracker(tracker, now)
      displayAction(doSim(tracker, action, now))
      now = now plus config.simulationResolution
    }
  }
}
