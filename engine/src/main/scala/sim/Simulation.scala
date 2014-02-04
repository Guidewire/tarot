package com.guidewire.tarot.sim

import org.joda.time.{Interval, Duration, DateTime}
import com.guidewire.tarot.{Action, Config, Tracker, UID}
import com.guidewire.tarot.util.UIDGenerator

/** A wrapper class used for machines and test suites that are generated by the
  * [[Simulation]], in order to avoid UID collisions with machines and suites
  * that actually exist.
  *
  * @constructor Create a [[Fake]], identified by a [[Long]].
  * @param value The value for this [[Fake]].
  */
private[sim]
sealed case class Fake(value: Long)

/** A [[UIDGenerator]] for [[Fake]] [[UID]]s to be used for the [[Simulation]]
  * in order to provide UIDs for machines and test suites that it generates
  * and avoid conflicts with the [[UID]]s of machines and suites that actually
  * exist. As long as only one [[FakeUIDGenerator]] is used, conflicts between
  * [[UID]]s is guaranteed to be prevented. Generated [[Fake]] [[UID]]s start
  * at a given value, and increment for every generated [[UID]].
  *
  * @constructor Creates a [[FakeUIDGenerator]] that will produce [[Fake]]
  *              [[UID]s, starting at a provided value and incrementing for
  *              each successive [[UID]].
  * @param curr The starting value of the [[Fake]] [[UID]].
  */
private[sim]
class FakeUIDGenerator(private[this] var curr: Long) extends UIDGenerator {
  def apply(): UID[_] = UID(Fake{curr += 1; curr})
}

/** Performs a simulation which applies a given sequence of [[Action]]s and
  * simulates how the state of the system would change as a result, as well
  * as how it might change from outside effects, such as incoming test suites.
  *
  * Each [[Action]] in the sequence is applied and its effect is simulated
  * over a fixed period of time, which is specified in the provided [[Config]].
  * The simulation step is performed by [[SimulationStepView$]].
  */
object Simulation {
  /** @param decisionPath The sequence of [[Action]]s that we want to simulate.
    * @param startTracker The current state of the system.
    * @param now The "current time". More specifically, whatever time we want
    *             at which we want to simulate the given decision path.
    * @param config The engine-wide configuration for Tarot.
    * @return Returns the state of the system after the simulation, and the
    *         sequence of actions that were able to be taken throughout the
    *         simulation. This would differ from the provided sequence of
    *         actions if the requested number of machines to power down
    *         was not able to be fulfilled.
    */
  def apply(decisionPath: Seq[Action],
            startTracker: Tracker,
            now: DateTime,
            config: Config): (Tracker, Seq[Action]) = {
    val uidGenerator = new FakeUIDGenerator(0L)
    val suiteGenerator = new InternalSuiteGenerator(config)
    val tracker = startTracker.clone
    var stepTime = now
    val actualAction =
      for (action <- decisionPath)
      yield {
        val t = stepTime
        stepTime = stepTime plus config.simulationResolution
        SimulationStepView(
          tracker, action, t, uidGenerator, suiteGenerator, config)
      }
    (tracker, actualAction)
  }
}