package com.guidewire.tarot

/** Immutable delta of number of machines of each kind
  *
  * Each value in [[delta]] is interpreted as follows:
  *  - if zero: nothing is done to the machines of the respective
  *    [[MachineKind]] (equivalent to key-value pair not being present in
  *    [[delta]])
  *  - if positive: that many machines are created and set to
  *    [[MachineState.POWERING_ON]] state.
  *  - if negative: attempt to shutdown machines that are in state
  *    [[MachineState.ACTIVE]] and not running any suites.
  *
  * [[sim.Simulation]] and [[sa.SimulatedAnnealing]] operate on
  * "action paths", represented by type `Seq[Action]`.
  * The actions are evenly spaced in time, with the spacing given by
  * [[Config.simulationResolution]].
  * The number of actions in a path is specified by
  * [[sa.SimulatedAnnealingConfig.simulationDepth]].
  *
  * The final "recommendation" from Tarot is an [[Action]].
  *
  * @param delta map from [[MachineKind]] UID to delta
  */
case class Action(delta: Map[UID[_], Int])
