package com.guidewire.tarot.sim

import scala.collection.{GenSet, mutable, SortedMap}
import org.joda.time.{DateTime, Interval}
import com.guidewire.tarot.{
  Action, Config, MachineState, SuiteState, Tracker, UID}
import com.guidewire.tarot.util.UIDGenerator

/**  A single simulation step.
  *
  *  Applies a given action to the system and simulates the effect that it has
  *  on the state of the system, as well as environmental effects such as test
  *  suites entering the test queue.
  *
  *  The simulation does the following things:
  *   - Advance the progress of running suites and machines powering on or off
  *   - Emulates TH's discipline for assigning test suites to machines
  *   - Apply the given [[Action]] to the system by powering machines on or off
  *
  *  @note The simulation mutates the provided [[Tracker]]. If you do not
  *        intend for this, be sure to first clone the tracker to make a copy
  *        before providing it to the simulation step.
  */
object SimulationStepView {
  /**  Performs a simulation step. Read object description for further details.
    *
    *  @param tracker The "current" (possibly simulated) state of the system
    *  @param action The [[Action]] that we want to apply of which we would
    *                like to simulate the effects.
    *  @param stepStart The beginning of the simulation step interval
    *  @param uidGenerator A [[util.UIDGenerator]] that generates [[UID]]s for
    *                      any test suites or machines that are produced
    *                      within the simulation.
    *  @param config A The engine-wide configuration for Tarot.
    *  @return Returns the action that was actually taken during the
    *          simulation, which could be different if the number of machines
    *          requested to be powered off could not be fulfilled.
    */
  def apply(tracker: Tracker,
            action: Action,
            stepStart: DateTime,
            uidGenerator: UIDGenerator,
            suiteGenerator: SuiteGenerator,
            config: Config): Action =
    new SimulationStepView(
      tracker, stepStart, uidGenerator, suiteGenerator, config) run action
}

private[sim]
class SimulationStepView private[SimulationStepView]
  (tracker: Tracker,
   stepStart: DateTime,
   uidGenerator: UIDGenerator,
   suiteGenerator: SuiteGenerator,
   config: Config) {

  private[this] val stepEnd = stepStart plus config.simulationResolution

  private[this]
  def powerOnMachine(kindUID: UID[_]): Unit = {
    val uid = uidGenerator()
    tracker.machine.add(uid, kindUID)
    tracker.machine.update(uid, stepStart, MachineState.POWERING_ON)
  }

  private[this]
  def powerOffMachines(kindUID: UID[_], count: Int): Int = {
    assert(count > 0)

    val busyMachines: GenSet[UID[_]] = for {
      suiteUID <- tracker.suite.list
      suite = tracker.suite.get(suiteUID).get
      point <- suite.life findPoint stepStart
      suiteParentUID <- point.value match {
        case SuiteState.RUNNING(parentUID) => Some(parentUID)
        case _ => None
      }
    } yield suiteParentUID

    val idleMachines = (
      for {
        machineUID <- tracker.machine.list &~ busyMachines
        machine = tracker.machine.get(machineUID).get
        if (machine.kindUID == kindUID)
        point <- machine.life findPoint stepStart
        if (point.value match {case MachineState.ACTIVE=>true; case _=>false})
      } yield machineUID
    ) take count

    for (uid <- idleMachines)
      tracker.machine.update(uid, stepStart, MachineState.POWERING_OFF)

    idleMachines.size
  }

  private[this]
  def applyAction(action: Action) = Action(
    for ((kindUID, delta) <- action.delta)
    yield kindUID -> (
      if (delta >= 0) {
        for (_ <- 0 until delta) powerOnMachine(kindUID)
        delta
      } else {
        -powerOffMachines(kindUID, -delta)
      }
    )
  )

  private[this]
  def advanceMachineProgress(): Unit =
    for {machineUID <- tracker.machine.list
         machine = tracker.machine.get(machineUID).get
    } {
      val nowPoint = machine.life(machine.life.size - 1)
      assert(nowPoint.time.compareTo(stepStart) <= 0)
      nowPoint.value match {
        case MachineState.POWERING_ON =>
          val end = nowPoint.time.plus(
                    config.machineKinds(machine.kindUID).startDuration)
          if (end.compareTo(stepEnd) < 0)
            tracker.machine.update(machineUID, end, MachineState.ACTIVE)
        case MachineState.POWERING_OFF =>
          val end = nowPoint.time.plus(
                    config.machineKinds(machine.kindUID).stopDuration)
          if (end.compareTo(stepEnd) < 0)
            tracker.machine.update(machineUID, end, MachineState.INACTIVE)
        case _ =>
      }
    }

  private[this]
  def advanceSuiteProgress(): Unit = {
    for {suiteUID <- tracker.suite.list
         suite = tracker.suite.get(suiteUID).get
    } {
      val nowPoint = suite.life(suite.life.size - 1)
      assert(nowPoint.time.compareTo(stepStart) <= 0)
      nowPoint.value match {
        case SuiteState.RUNNING(parentUID) =>
          assert(tracker.machine.get(parentUID).get
                        .life.findPoint(stepStart).get.value ==
                        MachineState.ACTIVE)
          val end =
            nowPoint.time plus config.suiteKinds(suite.kindUID).runDuration
          if (end.compareTo(stepEnd) < 0)
            tracker.suite.update(suiteUID, end, SuiteState.INACTIVE)
        case _ =>
      }
    }
  }

  private[this]
  def addIncomingSuites(): Unit = {
    for ((time, suiteKindUID) <-
         suiteGenerator(new Interval(stepStart, stepEnd))) {
      assert(stepStart.compareTo(time) <= 0)
      assert(stepEnd.compareTo(time) > 0)
      val suiteUID = uidGenerator()
      tracker.suite.add(suiteUID, suiteKindUID)
      tracker.suite.update(suiteUID, time, SuiteState.QUEUED)
    }
  }

  private[this]
  def machineLoads(): scala.collection.Map[UID[_], Int] = {
    val loads = mutable.Map[UID[_], Int]()
    for {suiteUID <- tracker.suite.list
         suite = tracker.suite.get(suiteUID).get
         nowPoint <- suite.life findPoint stepStart
    } nowPoint.value match {
      case SuiteState.RUNNING(parentUID) =>
        loads(parentUID) =
          loads.getOrElse(parentUID, 0) +
          config.suiteKinds(suite.kindUID).capacity
      case _ =>
    }
    loads
  }

  private[this]
  def machineFrees(): SortedMap[Int, Seq[UID[_]]] = {
    val loads = machineLoads()
    var frees = SortedMap[Int, mutable.ArrayBuffer[UID[_]]]()
    for {machineUID <- tracker.machine.list
         machine = tracker.machine.get(machineUID).get
         nowPoint <- machine.life findPoint stepStart
         if (nowPoint.value == MachineState.ACTIVE)
    } {
      val free =
        config.machineKinds(machine.kindUID).capacity -
        loads.getOrElse(machineUID, 0)
      assert(free >= 0)
      frees.get(free) match {
        case None =>
          frees = frees + (free -> mutable.ArrayBuffer(machineUID))
        case Some(oldList) =>
          oldList += machineUID
      }
    }
    frees
  }

  // TODO: check SuiteKind/MachineKind compatibility
  private[this]
  def runSuites() {
    var frees = machineFrees()
    for (suiteUID <- tracker.suite.queue(stepStart)) {
      val suite = tracker.suite.get(suiteUID).get
      assert(suite.life.findPoint(stepStart).get.value == SuiteState.QUEUED)

      val suiteLoad = config.suiteKinds(suite.kindUID).capacity
      frees.find(p => p._1 >= suiteLoad) match {
        case None =>
        case Some((_, candidates)) =>
          tracker.suite.update(suiteUID,
                               stepStart,
                               SuiteState.RUNNING(candidates(0)))
          frees = machineFrees()
      }
    }
  }

  private[SimulationStepView]
  def run(action: Action): Action = {
    val actualAction = applyAction(action)
    runSuites()
    advanceMachineProgress()
    advanceSuiteProgress()
    addIncomingSuites()

    actualAction
  }
}
