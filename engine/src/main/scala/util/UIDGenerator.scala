package com.guidewire.tarot.util

import com.guidewire.tarot.UID

/**  Generates UIDs for test suites and/or machines.
  *
  *  Typically used to create mock UIDs for [[sim.SimulationStepView$]],
  *  but can also be used for external simulators.
  */
trait UIDGenerator {
  /** Returns a new [[UID]] for simulated objects
    *
    * With the same [[UIDGenerator]] instance,
    * the returned [[UID]] instances should be unique.
    */
  def apply(): UID[_]
}
