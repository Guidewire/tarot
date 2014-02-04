package com.guidewire.tarot

import org.joda.time.Interval
import com.guidewire.tarot.sa.SimulatedAnnealingConfig

/** "Happiness function"
  *
  * Similar to a "cost function":
  * while "cost" should be minimized, "happiness" should be maximized.
  *
  * Implementations should be provided by the application.
  *
  * @see [[sa.SimulatedAnnealing]]
  */
trait HappyFunc {
  /** Returns the "goodness" of a collection of trackers.
    *
    * The higher the score, the better these trackers are.
    *
    * @param trackers Collection of trackers to evaluate
    * @param interval Time interval over which trackers are evaluated
    */
  def apply(trackers: Iterator[Tracker], interval: Interval): Double
}

/** Sample implementation of [[HappyFunc]]
  *
  * For use only by lazy applications.
  */
class SampleHappyFunc(config: Config,
                      saConfig: SimulatedAnnealingConfig) extends HappyFunc {
  private[this]
  def isMachineOn(v: MachineState.Value): Boolean =
    v match {
      case MachineState.POWERING_ON => true
      case MachineState.POWERING_OFF => true
      case MachineState.ACTIVE => true
      case _ => false
    }

  private[this]
  def isSuiteWaiting(v: SuiteState.Value): Boolean =
    v match {
      case SuiteState.RUNNING(_) => true
      case SuiteState.QUEUED => true
      case _ => false
    }

  private[this]
  def applyOne(tracker: Tracker, interval: Interval): Double = {
    val machineCosts =
      for (machine <- tracker.machine.list.toSeq
                             .map(tracker.machine.get(_).get))
      yield {
        val runDuration = machine.life.countTime(isMachineOn, interval)
        config.machineKinds(machine.kindUID).runCost * runDuration.getMillis()
      }

    val suiteWaitCosts =
      for (suite <- tracker.suite.list.toSeq.map(tracker.suite.get(_).get))
      yield {
        val waitDuration = suite.life.countTime(isSuiteWaiting, interval)
        saConfig.suiteWaitPrice * waitDuration.getMillis()
      }

    -suiteWaitCosts.sum - machineCosts.sum
  }

  def apply(trackers: Iterator[Tracker], interval: Interval): Double =
    trackers.map(applyOne(_, interval)).reduce(_ + _)
}
