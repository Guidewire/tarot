package com.guidewire.tarot.sa

import org.joda.time.DateTime

import com.guidewire.tarot.{Action, Config, HappyFunc, Tracker}

/** Simulated Annealing entry point */
object SimulatedAnnealing {
  /** Attempts to find the best action path
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
    *        Simulated annealing configurations
    *
    * @return An action path which, when applied to `startTracker`, produces
    *         a high score from `happyFunc`.
    */
  def apply(startTracker: Tracker,
            now: DateTime,
            happyFunc: HappyFunc,
            config: Config,
            saConfig: SimulatedAnnealingConfig): Seq[Action] = {
    val ai = new AnnealIterator(startTracker, now, happyFunc, config, saConfig)
    // Perform simulated annealing
    Corun(new TemperatureIterator(saConfig), ai, false)
    // Get the best action path
    ai.getBest
  }
}
