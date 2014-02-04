package com.guidewire.tarot.sa

import org.joda.time.DateTime

import com.guidewire.tarot.{Action, Config, HappyFunc, Tracker}

/** An iterator that iterates over iterations of simulated annealing.
  *
  * @constructor Produces an [[AnnealIterator]]
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
class AnnealIterator(startTracker: Tracker,
                     now: DateTime,
                     happyFunc: HappyFunc,
                     config: Config,
                     saConfig: SimulatedAnnealingConfig)
  extends Coroutine[(Boolean, Double), Boolean] {
  private[this]
  val mutator = new Mutator(config, saConfig)

  private[this]
  val evaluator = new Evaluator(startTracker, now, happyFunc, config, saConfig)

  private[this]
  val initSolution: Seq[Action] = {
    val zeroAction = new Action(config.machineKinds map (e => e._1 -> 0))
    0 until saConfig.simulationDepth map (_ => zeroAction)
  }

  private[this] var (bestScore: Double, best: Seq[Action]) =
    evaluator(initSolution)
  private[this] var here: Seq[Action] = best
  private[this] var hereScore: Double = bestScore

  /**  Returns the best score thus far.
    */
  def getBest = best

  /**  Performs an iteration of simulated annealing.
    *
    *  @return Optionally (though always) returns if a new best score was found
    */
  def next(data: (Boolean, Double)): Option[Boolean] = {
    val (didKick, temperature) = data
    // If kicked, flatten; else mutate
    val trythere: Seq[Action] = if (didKick)
        mutator.flatten(here)
      else
        mutator.scramble(here)
    // Evaluate the decision path, getting the score and the effective
    // decision path
    val (thereScore: Double, there: Seq[Action]) = evaluator(trythere)
    var gotNewBest = false
    // If the score is better than the current, or if our biased coin demands,
    if (thereScore >= hereScore || config.random.nextDouble() <= temperature) {
      // Set the score to be the current
      here = there
      hereScore = thereScore
      // If the score is better than the best score, we have a new best
      if (hereScore > bestScore) {
        gotNewBest = true
        best = here
        bestScore = hereScore
      }
    }
    Some(gotNewBest)
  }
}
