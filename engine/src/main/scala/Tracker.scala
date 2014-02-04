package com.guidewire.tarot

import scala.collection.mutable
import scala.util.Sorting
import org.joda.time.{DateTime, Duration}

import com.guidewire.tarot.timeseries.TimeSeries

/** Namespace of all states that each suite can be in
  *
  * =Summary of states=
  * {{{
  * |          | Incurring | Occupying  |
  * |   State  |  costs?   | a machine? |
  * +----------+-----------+------------+
  * | INACTIVE |    no     |    no      |
  * | QUEUED   |    yes    |    no      |
  * | RUNNING  |    yes    |    yes     |
  * }}}
  */
object SuiteState {
  /** Base trait of all suite states */
  sealed trait Value

  /** Suite is not in queue nor running on a machine */
  case object INACTIVE extends Value

  /** Suite is waiting in queue */
  case object QUEUED extends Value

  /** Suite is running on a machine
    *
    * @param parent
    *        [[UID]] of machine (a [[TrackObject]]) on which this suite is
    *        running
    */
  sealed case class RUNNING(val parent: UID[_]) extends Value
}

/** Namespace of all states that each machine can be in
  *
  * =Summary of states=
  * {{{
  * |              | Incurring | Can run |
  * |   State      |  costs?   | suites? |
  * +--------------+-----------+---------+
  * | INACTIVE     |    no     |   no    |
  * | POWERING_ON  |    yes    |   no    |
  * | POWERING_OFF |    yes    |   no    |
  * | ACTIVE       |    yes    |   yes   |
  * }}}
  */
object MachineState {
  /** Base trait of all machine states */
  sealed trait Value

  /** Machine is shutdown */
  case object INACTIVE extends Value

  /** Machine is transitioning from [[INACTIVE]] to [[ACTIVE]] */
  case object POWERING_ON extends Value

  /** Machine is transitioning from [[ACTIVE]] to [[INACTIVE]] */
  case object POWERING_OFF extends Value

  /** Machine is ready for work */
  case object ACTIVE extends Value
}

/** Factory object */
object Tracker {
  private[this]
  def suiteFactory(kindUID: UID[_], life: TimeSeries[SuiteState.Value]) =
    new TrackObject(kindUID, life)

  private[this]
  def machineFactory(kindUID: UID[_], life: TimeSeries[MachineState.Value]) =
    new TrackObject(kindUID, life)

  private[this]
  def suiteCanDrop(suite: TrackObject[SuiteState.Value]): Boolean = {
    val size = suite.life.size
    size == 0 || suite.life(size - 1).value == SuiteState.INACTIVE
  }

  private[this]
  def machineCanDrop(machine: TrackObject[MachineState.Value]): Boolean = {
    val size = machine.life.size
    size == 0 || machine.life(size - 1).value == MachineState.INACTIVE
  }

  /** Creates a new, empty [[Tracker]] */
  def apply(): Tracker = new Tracker(
    new SuiteTracker(
      mutable.Map[UID[_], TrackObject[SuiteState.Value]](),
      suiteFactory,
      suiteCanDrop
    ),
    new GenericTracker(
      mutable.Map[UID[_], TrackObject[MachineState.Value]](),
      machineFactory,
      machineCanDrop
    )
  )
}

/** Immutable "kind" of machine
  *
  * @param startDuration
  *        Time expected to be in [[MachineState.POWERING_ON]] state
  * @param stopDuration
  *        Time expected to be in [[MachineState.POWERING_OFF]] state
  * @param capacity
  *        Upper limit on sum of capacities of suites ([[SuiteKind.capacity]])
  *        running on this kind of machine
  * @param runCost
  *        "Cost" of running this kind of machine, used by [[SampleHappyFunc]]
  *
  * @see [[Config.machineKinds]]
  * @see [[TrackObject.kindUID]]
  * @todo Add "capabilities" (i.e. the characteristics that determine
  * the kinds of suites that this kind of machines can run).
  */
case class MachineKind(startDuration: Duration,
                       stopDuration: Duration,
                       capacity: Int,
                       runCost: Double)

/** Immutable "kind" of suite
  *
  * @param runDuration
  *        Time expected to be in [[SuiteState.RUNNING]] state
  * @param capacity
  *        Amount of load on a machine, related to [[MachineKind.capacity]]
  *
  * @see [[Config.suiteKinds]]
  * @see [[TrackObject.kindUID]]
  * @todo Add "capabilities" (i.e. the software requirements
  * of this kind of suites: OS, web server, DB, browser, JVM, and so forth).
  */
case class SuiteKind(runDuration: Duration,
                     capacity: Int)

/** Immutable representation of a suite or a machine
  *
  * Instances should be managed via [[Tracker.machine]] or [[Tracker.suite]].
  * Tarot will not find instances that are created outside of a tracker.
  *
  * @constructor only [[Tracker]] should directly construct this
  * @tparam STATE
  *         Type of states that this [[TrackObject]] has.
  *         Typically either [[MachineState.Value]] (for machines)
  *         or [[SuiteState.Value]] (for suites).
  * @param kindUID
  *        [[UID]] referring either to a [[MachineKind]] (for machines)
  *        or a [[SuiteKind]] (for suites).
  *        Use [[Config.machineKinds]] or [[Config.suiteKinds]] to
  *        resolve [[kindUID]] to the "kind" instances.
  *        Machines or suites of the same kind should have equal [[kindUID]].
  * @param life
  *        History of states
  *
  * @todo keep constructor private to Tracker-related objects
  */
case class TrackObject[STATE](kindUID: UID[_], life: TimeSeries[STATE])

/** Mutable history of a collection of [[TrackObject]] instances
  *
  * A [[GenericTracker]] is the complete map
  * from a [[UID]] to a [[TrackObject]].
  *
  * Because [[TrackObject]] instances capture their histories of states,
  * a [[GenericTracker]] captures the history of a collection.
  *
  * @tparam STATE
  *         State type of contained [[TrackObject]] instances
  * @constructor Used only by [[Tracker]]
  *
  * @todo Could take a [[Config]] for [[UID]] validation
  * @todo Keep constructor private to [[Tracker]]
  */
sealed class GenericTracker[STATE]
  (objects: mutable.Map[UID[_], TrackObject[STATE]],
   factory: (UID[_], TimeSeries[STATE]) => TrackObject[STATE],
   canDrop: TrackObject[STATE] => Boolean) {
  /** Updates state of a tracked object
    *
    * @param objectUID [[UID]] of [[TrackObject]] to be updated
    * @param time When the desired [[TrackObject]] should change to new state
    * @param state New state
    *
    * @throws NoSuchElementException if `objectUID` is not known
    */
  def update(objectUID: UID[_], time: DateTime, state: STATE): Unit = {
    val old = objects(objectUID)
    objects += objectUID -> factory(old.kindUID, old.life.add(time, state))
  }

  override
  def clone() = new GenericTracker[STATE](objects.clone(), factory, canDrop)

  /** Returns all known [[UID]] instances */
  def list: scala.collection.Set[UID[_]] = objects.keySet

  /** Returns the desired [[TrackObject]] or `None` if not found
    *
    * @param objectUID [[UID]] of desired [[TrackObject]]
    */
  def get(objectUID: UID[_]): Option[TrackObject[STATE]] =
    objects.get(objectUID)

  /** Creates and registers a new [[TrackObject]]
    *
    * [[TrackObject.life]] of the new [[TrackObject]] will be empty.
    *
    * @param objectUID [[UID]] of new [[TrackObject]]
    * @param kindUID Passed to [[TrackObject.kindUID]]
    *
    * @throws IllegalArgumentException if `objectUID` is already registered
    */
  def add(objectUID: UID[_], kindUID: UID[_]): Unit = {
    assert(!objects.contains(objectUID))
    objects += objectUID -> factory(kindUID, TimeSeries[STATE]())
  }

  def prune(): Unit = {
    val newObjects =
      for {(uid, old) <- objects if (!canDrop(old))}
      yield uid -> factory(old.kindUID, old.life.prune())
    objects.clear()
    objects ++= newObjects
  }
}

/** Mutable history of a collection of suites
  *
  * A [[SuiteTracker]] is the complete map
  * from a [[UID]] to a suite (which is a [[TrackObject]] instance).
  *
  * @constructor Used only by [[Tracker]]
  * @todo Could take a [[Config]] for [[UID]] validation
  * @todo Keep constructor private to [[Tracker]]
  */
class SuiteTracker
  (objects: mutable.Map[UID[_], TrackObject[SuiteState.Value]],
   factory: (UID[_], TimeSeries[SuiteState.Value])
             => TrackObject[SuiteState.Value],
   canDrop: TrackObject[SuiteState.Value] => Boolean)
 extends GenericTracker[SuiteState.Value](objects, factory, canDrop) {

  override
  def clone() = new SuiteTracker(objects.clone(), factory, canDrop)
  def queue(dt: DateTime): Seq[UID[_]] = {
    implicit val queueOrdering = new Ordering[UID[_]] {
      def compare(a: UID[_], b: UID[_]): Int = {
        val aDate = get(a).get.life.findPoint(dt).get.time
        val bDate = get(b).get.life.findPoint(dt).get.time
        aDate compareTo bDate
      }
    }

    Sorting.stableSort((for {
      uid <- list
      suite = get(uid).get
      point <- suite.life.findPoint(dt) // Add assertions later
      state = point.value
      if state == SuiteState.QUEUED
    } yield uid).toSeq).toSeq
  }
}

/** Mutable "world" history
  *
  * Tracks histories of suites and machines
  *
  * [[Tracker]] itself does not do much;
  * the actual work is deferred to [[suite]] and [[machine]].
  */
class Tracker private[Tracker]
  (val suite: SuiteTracker,
   val machine: GenericTracker[MachineState.Value]) {
  override
  def clone(): Tracker = new Tracker(suite.clone(), machine.clone())

  /** Removes inactive suites and machines
    * and removes old history of active ones.
    */
  def prune(): Unit = {
    suite.prune()
    machine.prune()
  }
}
