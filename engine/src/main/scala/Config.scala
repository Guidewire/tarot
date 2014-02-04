package com.guidewire.tarot

import scala.util.Random

import org.joda.time.Duration

/** Mutable shared configuration
  *
  * The only mutable member of [[Config]] is [[random]].
  *
  * Members prefixed with "`SA`" are exclusively
  * used by [[sa.SimulatedAnnealing]].
  *
  * @constructor Defines a new configuration
  * @param machineKinds
  *        Complete map from [[UID]] to [[MachineKind]] instances
  * @param suiteKinds
  *        Complete map from [[UID]] to [[SuiteKind]] instances
  * @param simulationResolution
  *        Gap between [[Action]] elements of an action path
  * @param simulationDepth
  *        Length of [[Action]] paths
  * @param averageSuiteArrivalRate
  *        Average number of simulated suites arriving per millisecond,
  *        used by [[sim.SuiteGenerator]]
  * @param suiteWaitPrice
  *        "Cost" of a suite waiting, used by [[SampleHappyFunc]]
  * @param SAEvaluateRepeats
  *        Number of simulation re-runs per SA evaluation
  * @param SAMutateSize
  *        Maximum amount by which each value of [[Action.delta]] can mutate
  * @param SAMutateProbability
  *        Probability of mutating an [[Action]] in an action path
  * @param SATemperatureCutoff
  *        Low temperature at which SA will halt
  * @param SATemperatureDecay
  *        SA temperature is multiplied by this factor on each iteration
  * @param randomSeed
  *        Seed for [[random]]
  * @param notifyProgressListeners
  *        Called by [[sa.SimulatedAnnealing]] to report progress
  */
class Config(val machineKinds: Map[UID[_], MachineKind],
             val suiteKinds: Map[UID[_], SuiteKind],
             val simulationResolution: Duration,
             val averageSuiteArrivalRate: Double,
             randomSeed: Long) {
  /** Shared `Random` instance */
  val random = new Random(randomSeed)

  def suiteKindsFor(machineKindUID: UID[_]): Iterable[UID[_]] = {
    assert(machineKinds contains machineKindUID)
    suiteKinds.keys // TODO: return an interesting subset
  }
}
