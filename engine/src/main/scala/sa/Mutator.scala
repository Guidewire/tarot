package com.guidewire.tarot.sa

import scala.collection.mutable

import com.guidewire.tarot.{Action, Config, UID}

/**  A class to provide utilities for mutating [[Action]]s in a decision path.
  *
  *  @constructor Produces a [[Mutator]].
  *  @param config Shared configuration
  *  @param saConfig Simulated annealing configuration
  */
private[sa]
class Mutator(config: Config, saConfig: SimulatedAnnealingConfig) {
  /**  Produces a random number by which to mutate an action.
    *
    *  @param limit The maximum amount of machines to add or remove
    */
  private[this]
  def randomInt(limit: Int): Int =
    if (config.random.nextDouble() < saConfig.SAMutateProbability)
      config.random.nextInt(2 * limit + 1) - limit
    else
      0

  /**  Mutates a sequence of action by slightly adding or removing
    *  a number of machines from each type of machine.
    *
    *  @param actionPath A sequence of [[Action]]s
    *
    *  @return Returns the mutated sequence of actions.
    */
  def scramble(actionPath: Seq[Action]): Seq[Action] =
    for ((action, index) <- actionPath.zipWithIndex)
    yield
      if (index % saConfig.SAMutateDensity == 0)
        new Action(
          for ((uid, delta) <- action.delta) yield
            uid -> (delta + randomInt(saConfig.SAMutateSize))
        )
      else action

  /**  Averages all of the actions in a sequence into a sequence of actions
    *  where each action adds or removes the average number of machines for
    *  the type of machine.
    *
    *  @param actionPath A sequence of actions
    *
    *  @return Returns the averaged action
    */
  def flatten(actionPath: Seq[Action]): Seq[Action] = {
    // Make a new map from UID to a Seq[Int], so that we can capture all
    // of the deltas for each machine in the action path
    val collectedDeltas = mutable.Map[UID[_], Seq[Int]]()
    for {action <- actionPath
        (uid, delta) <- action.delta}
      collectedDeltas(uid) = collectedDeltas.get(uid) match {
        case None => Seq[Int](delta)
        case Some(uidDeltas) => uidDeltas :+ delta
      }
    // Average the deltas for each machine
    val averagedDeltas = for ((uid, deltas) <- collectedDeltas)
                         yield uid -> deltas.sum / deltas.size
    // Produce a new action that has the same machines, but the
    // averaged deltas.
    for (action <- actionPath)
    yield new Action(
      for ((uid, delta) <- action.delta)
      yield uid -> averagedDeltas(uid)
    )
  }
}
