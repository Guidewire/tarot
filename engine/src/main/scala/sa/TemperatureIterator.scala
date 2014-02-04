package com.guidewire.tarot.sa

import com.guidewire.tarot.Config

/** An iterator for producing temperature values for iterations of
  * simulated annealing.
  *
  * @constructor Produces a TemperatureIterator.
  *
  * @param saConfig Shared configuration
  */
private[sa]
class TemperatureIterator(saConfig: SimulatedAnnealingConfig)
  extends Coroutine[Boolean, (Boolean, Double)] {
  // Temperature starts at 1.0
  private[this] var t = 1.0
  private[this] var lastInterestingTemperature = 1.0

  /** "Kicks" up the temperature, increasing it to prolong the annealing.
    */
  private[this]
  def kick(): Unit = {
    t = (t + lastInterestingTemperature) / 2.0
    lastInterestingTemperature = t
  }

  /** Provides the next simulated annealing temperature.
    *
    * @param gotNewBest If the simulated annealing algorithm got a new
    *                   best solution.
    *
    * @return Optionally returns if the temperature was kicked, and the next
    *         temperature.
    */
  private[this]
  def nextTemperature(gotNewBest: Boolean): Option[(Boolean, Double)] = {
    assert(0.0 <= t && t <= 1.0)
    assert(t <= lastInterestingTemperature &&
           lastInterestingTemperature <= 1.0)
    if (gotNewBest)
      lastInterestingTemperature = t
    val didKick = 2.0 * t < lastInterestingTemperature
    if (didKick)
      kick()
    if (t < saConfig.SATemperatureCutoff) {
      None
    } else {
      t *= saConfig.SATemperatureDecay
      Some((didKick, t))
    }
  }

  /** Notify listeners that are subscribed to the simulated annealing
    * process with the progress.
    *
    * @param data Optionally returns if the temperature was kicked, and the
    *             next temperature
    */
  private[this]
  def notifyProgress(data: Option[(Boolean, Double)]): Unit =
    data match {
      case None => saConfig.notifyProgressListeners(1.0)
      case Some((didKick, t)) =>
        if (didKick)
          saConfig.notifyProgressListeners(
            Math.log(t) / Math.log(saConfig.SATemperatureCutoff)
          )
    }

  /** Provides the next iterator value, and notifies progress listeners.
    *
    * @param gotNewBest If the simulated annealing algorithm got a new beest
    *                   solution.
    * @return Optionally return if the temperature was kicked, and the next
    *         temperature.
    *
    */
  def next(gotNewBest: Boolean): Option[(Boolean, Double)] = {
    val data = nextTemperature(gotNewBest)
    notifyProgress(data)
    data
  }
}
