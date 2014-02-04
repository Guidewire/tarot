package com.guidewire.tarot

import akka.actor._

import scala.util.Random
import scala.concurrent.duration._
import scala.collection.mutable
import scala.language.postfixOps
import org.joda.time.DateTime

import spray.util.SprayActorLogging
import spray.json.DefaultJsonProtocol

trait DataFeedProvider {
  def dataFeedProvider: ActorRef
}

trait SimulatorProvider {
  def simulator: ActorRef
}

object DataFeedActor {
  case class SuiteQueueSuiteKindStatus(id:String, size:Int)
  case class SuiteQueueMachineKindStatus(id:String, size:Int, suiteKindStatus:Iterable[SuiteQueueSuiteKindStatus])
  case class SuiteQueueInformation(size: Int, machineKindStatus:Iterable[SuiteQueueMachineKindStatus], rate: Double)

  case class MachineMachineKindStatusInformation(id:String, count:Int)
  case class MachineInformation(count: Int, machineKindStatus:Iterable[MachineMachineKindStatusInformation])

  case object RequestDataFeedInformation
  case class DataFeedInformationResponse(queue: SuiteQueueInformation, machines: MachineInformation)

  case class RegisterDataFeedListener(listener: ActorRef)
  case class SuiteQueueEvent(queue: SuiteQueueInformation)
  case class MachineEvent(machines: MachineInformation)
  case class UnregisterDataFeedListener(listener: ActorRef)

  object Serialization extends DefaultJsonProtocol {
    implicit val suite_queue_suite_kind_status_format = jsonFormat2(SuiteQueueSuiteKindStatus)
    implicit val suite_queue_machine_kind_status_format = jsonFormat3(SuiteQueueMachineKindStatus)
    implicit val suite_queue_information_format = jsonFormat3(SuiteQueueInformation)

    implicit val machine_machine_kind_status_information_format = jsonFormat2(MachineMachineKindStatusInformation)
    implicit val machine_information_format = jsonFormat2(MachineInformation)
  }
}

object SimulatorActor {
  case object StartUp
  case object ShutDown
  case object TickUpdate
  case object TickRunPass
  case class AddSuites(kinds:Iterable[String], count: Int)

  def apply(engine: ActorRef, name:String)(implicit system: ActorSystem) =
    system.actorOf(Props(new SimulatorActor(engine)), name)
}

class SimulatorActor(engine: ActorRef) extends Actor with SprayActorLogging {
  import Engine._
  import EngineActor._
  import AkkaUtils._
  import DataFeedActor._
  import SimulatorActor._

  implicit val executionContext = context.dispatcher

  private[this] val simulator = Simulator(log)

  private[this] val dataFeedListeners = mutable.LinkedHashSet[ActorRef]()
  private[this] def notifyDataFeedListeners(event:SuiteQueueEvent) = dataFeedListeners.foreach(_ ! event)
  private[this] def notifyDataFeedListeners(event:MachineEvent) = dataFeedListeners.foreach(_ ! event)

  def produceSuiteQueueInformation(): SuiteQueueInformation = {
    val size = simulator.queueSize()
    val suite_sizes_for_machine_kind = simulator.countSuitesForEachMachineKind()
    val machine_kind_statuses =
      for {
        (machine_uid, suite_kind_to_size_map) <- suite_sizes_for_machine_kind
        id = machine_uid.friendlyValue
        suite_kind_statuses = for {
          (suite_uid, size) <- suite_kind_to_size_map
          id = suite_uid.friendlyValue
        } yield SuiteQueueSuiteKindStatus(id, size)
        total_size = suite_kind_statuses.foldLeft(0)(_ + _.size)
      } yield SuiteQueueMachineKindStatus(id, total_size, suite_kind_statuses)
    val suiteRate = simulator.incomingSuiteRate()
    SuiteQueueInformation(size, machine_kind_statuses, suiteRate)
  }

  def produceMachineInformation(): MachineInformation = {
    val machines = simulator.countMachinesRunningForEachKind()
    val machine_kind_status =
      for {
        (uid, count) <- machines
        id = uid.friendlyValue
      } yield MachineMachineKindStatusInformation(id, count)
    MachineInformation(simulator.countMachinesRunning(), machine_kind_status)
  }

  def incorporateEngineResults(results: Results): Unit = {
    simulator.prepareAction(results.decision)
  }

  def receive = {
    case StartUp =>
      simulator.run() // Should probably make sure that if other messages
                      // received that they don't work unless sim is running

      //Register the simulator with the engine so we can receive notifications
      //when it has completed a pass and so we can simulate what happens should
      //we use the results.
      engine ! RegisterFinishedListener(self)

      //This is deliberate -- it alleviates pressure if work gets backed up
      //for whatever reason and this prevents a backlog of ticks from building
      //up b/c it will schedule a new tick only after the last one was processed.
      context.system.scheduler.scheduleOnce(0 seconds, self, TickUpdate)
      context.system.scheduler.scheduleOnce(0 seconds, self, TickRunPass)

    case ShutDown =>
      // Maybe notify others too?
      simulator.stop()

    case TickUpdate =>
      engine ! Update(EngineUpdate(simulator.getTracker()))
      notifyDataFeedListeners(SuiteQueueEvent(produceSuiteQueueInformation()))
      notifyDataFeedListeners(MachineEvent(produceMachineInformation()))
      context.system.scheduler.scheduleOnce(1 seconds, self, TickUpdate)

    case TickRunPass =>
      engine ! RunPass(simulator.getNow())
      context.system.scheduler.scheduleOnce(5 seconds, self, TickRunPass)

    case AddSuites(kinds, count) =>
      log info s"Adding $count suites to the following suite kinds: $kinds"
      simulator.addSuites(kinds, count)

    case FinishedEvent(results) =>
      log info s"Incorporating $results into the simulator"
      incorporateEngineResults(results)

    case RequestDataFeedInformation =>
      sender ! DataFeedInformationResponse(produceSuiteQueueInformation(), produceMachineInformation())

    case RegisterDataFeedListener(listener) =>
      dataFeedListeners add listener

    case UnregisterDataFeedListener(listener) =>
      dataFeedListeners remove listener
  }
}
