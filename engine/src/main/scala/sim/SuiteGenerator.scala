package com.guidewire.tarot.sim

import org.joda.time.{DateTime, Interval}
import com.guidewire.tarot.UID

trait InjectableSuiteGenerator {
  def injectAdditionalSimulatedSuites(suiteKind:UID[_], count:Int): Unit
}

trait SuiteGenerator {
  def apply(interval: Interval): Seq[(DateTime, UID[_])]
}
