package com.guidewire.tarot

import java.util.concurrent.Executors
import java.util.concurrent.atomic.{AtomicReference, AtomicBoolean}
import scala.collection.mutable
import org.joda.time.{Duration, DateTime}

import com.guidewire.tarot.common.ThreadUtil.asCallable
import com.guidewire.tarot.common.{Loggable, Logging}
import com.guidewire.tarot.sa.{SimulatedAnnealing, SimulatedAnnealingConfig}

class Listeners[S] {
  private[this] val listeners = mutable.Set[Engine.Listener[S]]()

  def notify(signal: S): Unit =
    listeners.foreach((_: Engine.Listener[S]).callBack(signal))
  def add(listener: Engine.Listener[S]): Unit = listeners add listener
  def remove(listener: Engine.Listener[S]): Unit = listeners remove listener
}

trait Engine { this:Logging =>
  import Engine._

  private[this] val execPool = Executors.newFixedThreadPool(1)

  private[this] val running: AtomicBoolean = new AtomicBoolean(false)

  protected
  var result: Results = Results(Action(Map()), new ImplementationResults {})

  val progressListeners = new Listeners[ProgressUpdate]
  val finishedListeners = new Listeners[Results]
  val newBestListeners = new Listeners[NewBestResults]

  def config: Config

  def run(now: DateTime): Boolean = {
    if (!running.getAndSet(true)) {
      execPool.submit(asCallable({
        try {
          result = executeOnce(now)
        } catch {
          case t:Throwable =>
            t.printStackTrace()
            System.exit(1)
        } finally {
          running.set(false)
        }
      }))
      true
    } else {
      false
    }
  }

  def currentResults: Results = result
  def shutdown(): Unit = execPool.shutdownNow()

  def update(update: EngineUpdate): Unit
  protected def executeOnce(now: DateTime): Results
}

class SimulatedAnnealingEngine extends Engine { this:Logging =>
  private[this] var currTracker = Tracker()

  val config = new Config(
    machineKinds=Map(
      UID("Windows 2008 R2 x86_64") -> MachineKind(
        startDuration=new Duration(100L),
        stopDuration=new Duration(100L),
        capacity=1,
        runCost=1.0
      ),
      UID("CentOS 6.4 x86_64") -> MachineKind(
        startDuration=new Duration(10L * 1000L),
        stopDuration=new Duration(5L * 1000L),
        capacity=10,
        runCost=50.0
      )
    ),
    suiteKinds=Map(
      UID("Tomcat 7, H2") -> SuiteKind(
        runDuration=new Duration(1000L * 2L),
        capacity=1
      ),
      UID("JBoss 6, H2") -> SuiteKind(
        runDuration=new Duration(1000L * 15L),
        capacity=10
      )
    ),
    simulationResolution=new Duration(1000L),
    averageSuiteArrivalRate=0.0,
    randomSeed=12345L
  )

  val saConfig = SimulatedAnnealingConfig(
    simulationDepth=60,
    suiteWaitPrice=50.0,
    SAEvaluateRepeats=1,
    SAMutateDensity=20,
    SAMutateSize=1,
    SAMutateProbability=1.0,
    SATemperatureCutoff=0.02,
    SATemperatureDecay=0.900,
    notifyProgressListeners=
      (progress: Double) =>
        progressListeners.notify(Engine.ProgressUpdate(progress, null))
  )

  def update(update: Engine.EngineUpdate): Unit = {
    // Vroom vroom -- race conditions
    currTracker = update.tracker
  }//log.info("Engine is updating...")

  def executeOnce(now: DateTime): Engine.Results = {
    log info "Executing simulated annealing pass..."
    val actionPath = SimulatedAnnealing(
      currTracker,
      now,
      new SampleHappyFunc(config, saConfig),
      config,
      saConfig
    )

    log info "Simulated annealing pass complete"
    /* Could do this:
     * var delta = actionPath.iterator.map(action => action.delta)
     *                       .map(delta => delta(UID("Machine Type A")))
     *                       .filter(_ != 0).toList.headOption.getOrElse(0)
     *
     * will use first non-zero delta on "Machine Type A" as final decision
     */

    val result = Engine.Results(actionPath(0),
                                new Engine.ImplementationResults {})
    finishedListeners.notify(result)
    result
  }
}



object Engine {
  case class EngineUpdate(tracker: Tracker)
  trait Listener[S] {
    def callBack(signal: S): Unit
  }

  trait ImplementationUpdate
  trait ImplementationResults

  case class ProgressUpdate(percentageDone: Double,
                            related: ImplementationUpdate)
  case class Results(decision: Action, related: ImplementationResults)
  case class NewBestResults(decision: Action, related: ImplementationResults)

  private[this] val engine = new AtomicReference[Engine]()

  def apply(loggable:Loggable): Engine = {
    val new_engine = create(loggable)
    if (engine.compareAndSet(null, new_engine)) {
      new_engine
    } else {
      engine.get()
    }
  }

  def create(loggable:Loggable): Engine =
    new SimulatedAnnealingEngine() with Logging {
      val log = loggable
    }
}
