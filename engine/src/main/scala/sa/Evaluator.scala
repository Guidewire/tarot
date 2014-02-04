package com.guidewire.tarot.sa

import scala.collection.mutable
import org.joda.time.{DateTime, Interval}

import com.guidewire.tarot.{Action, Config, HappyFunc, Tracker}
import com.guidewire.tarot.sim.Simulation

/**  A class for evaluating decision paths.
  *
  * @constructor Produces an [[Evalutor]]
  *
  * @param startTracker
  *        Initial "world state" to which actions are (virtually) applied
  * @param now
  *        Moment in time at which the first action is applied
  * @param happyFunc
  *        Defines the "best" action path
  * @param config
  *        Shared configuration
  * @param saConfig
  *        Simulated annealing configuration
  */
private[sa]
class Evaluator(startTracker: Tracker,
                now: DateTime,
                happyFunc: HappyFunc,
                config: Config,
                saConfig: SimulatedAnnealingConfig) {
  private[this]
  val actionPathInterval: Interval = {
    val lengthMillis: Long =
      config.simulationResolution.getMillis() * saConfig.simulationDepth.toLong
    new Interval(now, now.plus(lengthMillis))
  }

  /**  Cuts off machine removal actions in an action path, such that it is no
    *  lower than the lowest remove action in the action paths produced from
    *  simulations.
    *
    *  @param actionPath
    *         The proposed action path.
    *  @param otherActionPaths
    *         The action paths produced from multiple runs of the simulation.
    *  @return Return the action path, with actions cut off at the lowest
    */
  private[this]
  def normalizeActionPath(actionPath: Seq[Action],
                          otherActionPaths: Seq[Seq[Action]]): Seq[Action] = {
    // For each action in the proposed action path
    for (i <- 0 until actionPath.size)
    // Produce a new action
    yield new Action(
      for ((uid, delta) <- actionPath(i).delta)
      yield uid -> (
        // If removing machines
        if (delta < 0) {
          // Get the other deltas for this machine
          val otherDeltas: Seq[Int] =
            for {ap <- otherActionPaths
                 otherDelta <- ap(i).delta.get(uid)}
            yield otherDelta
          // Set the newDelta to be the minimum of the actions for this machine
          val newDelta = otherDeltas.min
          // delta is ensured to be <= newDelta, since the simulation
          // will never give us an action removing more machines than
          // we specified.
          assert(delta <= newDelta && newDelta <= 0)
          newDelta
        } else {
          delta
        }
      )
    )
  }

  /** Evaluates the provided action path.
    *
    * @param actionPath A sequence of [[Action]]s
    * @return Returns the score of the action path, and the adjusted
    *         action path produced by the [[Simulation]].
    */
  def apply(actionPath: Seq[Action]): (Double, Seq[Action]) = {
    val trackers = mutable.ArrayBuffer[Tracker]()
    val actualActionPaths = mutable.ArrayBuffer[Seq[Action]]()
    // Run the simulation a number of times
    for (_ <- 0 until saConfig.SAEvaluateRepeats) {
      val (tracker, actualActionPath) =
        Simulation(actionPath, startTracker, now, config)
      trackers += tracker
      actualActionPaths += actualActionPath
    }
    (
      // Get the happiness score from each simulation and normalize the
      // action paths
      happyFunc(trackers.iterator, actionPathInterval),
      normalizeActionPath(actionPath, actualActionPaths)
    )
  }
}
