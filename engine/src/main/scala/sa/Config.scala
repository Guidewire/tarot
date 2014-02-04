package com.guidewire.tarot.sa

/** Immutable simulated annealing tunables */
case class SimulatedAnnealingConfig(
  simulationDepth: Int,
  suiteWaitPrice: Double,
  SAEvaluateRepeats: Int,
  SAMutateDensity: Int,
  SAMutateSize: Int,
  SAMutateProbability: Double,
  SATemperatureCutoff: Double,
  SATemperatureDecay: Double,
  notifyProgressListeners: Double => Unit
)
