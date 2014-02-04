package com.guidewire.tarot

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers._
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith

import org.joda.time.{DateTime, Duration, Interval}
import com.guidewire.tarot.sa.SimulatedAnnealingConfig

@RunWith(classOf[JUnitRunner])
class SampleHappyFuncTest extends FunSuite
                          with ShouldMatchers {
  test("empty tracker should have zero score") {
    val config = new Config(Map(), Map(), new Duration(0L), 0.0, 0L)
    val saConfig = new SimulatedAnnealingConfig(
      0, 0.0, 0, 0, 0, 0.0, 0.0, 0.0, null
    )

    new SampleHappyFunc(config, saConfig)
      .apply(Seq(Tracker()).iterator,
             new Interval(-1000L, 1000L)) should equal (0.0)
  }

  test("running machines should lower the score") {
    val config = new Config(
      Map(
        UID("kind-0") ->
        MachineKind(new Duration(0L), new Duration(0L), 0, 3.5)
      ),
      Map(),
      new Duration(0L),
      0.0,
      0L
    )
    val saConfig = new SimulatedAnnealingConfig(
      0, 0.0, 0, 0, 0, 0.0, 0.0, 0.0, null
    )

    val happy = new SampleHappyFunc(config, saConfig)
    val tracker = Tracker()
    def h(): Double = happy(Seq(tracker).iterator, new Interval(0L, 1000L))
    def u(n: String, t: Long, s: MachineState.Value) {
      tracker.machine.update(UID(n), new DateTime(t), s)
    }

    h() should equal (0.0)

    tracker.machine.add(UID("hello-0"), UID("kind-0"))
    tracker.machine.add(UID("bye-0"), UID("kind-0"))

    u("hello-0", 300L, MachineState.POWERING_ON)
    u("hello-0", 400L, MachineState.ACTIVE)
    u("hello-0", 700L, MachineState.POWERING_OFF)
    u("hello-0", 800L, MachineState.INACTIVE)

    u("bye-0", 100L, MachineState.POWERING_ON)
    u("bye-0", 101L, MachineState.ACTIVE)
    u("bye-0", 102L, MachineState.POWERING_OFF)
    u("bye-0", 103L, MachineState.INACTIVE)

    h() should equal (-1760.5)
  }
}
