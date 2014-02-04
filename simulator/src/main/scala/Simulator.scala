package com.guidewire.tarot

import java.util.concurrent.{TimeUnit, Callable, Executors}
import java.util.concurrent.atomic.{AtomicReference, AtomicBoolean, AtomicInteger}
import org.joda.time.{Interval, Duration, DateTime}

import com.guidewire.tarot.common.{Loggable, Logging}
import com.guidewire.tarot.sim._
import com.guidewire.tarot.chart._
import com.guidewire.tarot.util.UIDGenerator
import com.guidewire.tarot.metrics.MetricsLogParser
import com.guidewire.tarot.chart.{HistogramSeries, Chart}

trait Simulator { this:Logging =>
  private[this] val THREAD_TICK_TIME = 1000L

  private[this]
  sealed case class NotReal(value: Long)

  private[this]
  class NotRealUIDGenerator(private[this] var curr: Long)
  extends UIDGenerator {
    def apply(): UID[_] = UID(NotReal{curr += 1; curr})
  }

  private[this] val execPool = Executors.newScheduledThreadPool(1)
  private[this] val running = new AtomicBoolean(false)
  private[this] val action = new AtomicReference[Action](Action(Map[UID[_], Int]()))
  private[this] val tracker = new AtomicReference[Tracker](Tracker())
  private[this] val suiteInflux = new AtomicInteger(0)
  private[this] val now = new AtomicReference[DateTime](new DateTime(0L))
  private[this] val NO_OP_ACTION = Action(Map[UID[_], Int]())
  private[this] val uidGenerator = new NotRealUIDGenerator(0L)
  private[this] val config = new Config(
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
    randomSeed=31415L
  )

  val th_metrics_series = (
    for {
      file_name <- Seq("tarot-queue-2013-6-21.log", "tarot-queue-2013-6-26.log")
      log = MetricsLogParser("graph-sandbox/examples/" + file_name)
      log_pairs = log.producePairs()
      pairs = HistogramSeries(file_name, 5.minutes)(log_pairs:_*).pairs
    }
    yield pairs map {
      case(_, count) => (count / 10.0D).toInt
    }
  ).flatten

  private[this]
  val suiteGenerator = new CollectiveSuiteGenerator(config, Map(
    UID("Tomcat 7, H2") -> new SeriesSuiteGenerator(
      config,
      UID("Tomcat 7, H2"),
      th_metrics_series
    ),
    UID("JBoss 6, H2") -> new SeriesSuiteGenerator(
      config,
      UID("JBoss 6, H2"),
      th_metrics_series
    )
  )) {
    override def apply(interval: Interval): Seq[(DateTime, UID[_])] = {
      val suites = super.apply(interval)
      suiteInflux.set(suites.size)
      suites
    }
  }

  private[this]
  def executeOnce(): Tracker = {
    val tracker = this.tracker.get().clone()
    tracker.prune()

    val action = this.action.get()
    this.action.set(new Action(Map()))
    println(s"Applying the action $action")
    println(s"Running with ${countMachinesRunning()} machines!")
    SimulationStepView(
      tracker, action, now.get(), uidGenerator, suiteGenerator, config)
    tracker
  }

  private[this]
  def tick(): Unit = {
    try {
      import com.guidewire.tarot.SuiteState
      import scala.language.reflectiveCalls

      log info "[SIMULATION]: Performing a simulation step..."
      val startQueueSize = queueSize()
      tracker.set(executeOnce())
      val newSuites = queueSize() - startQueueSize
      log info "[SIMULATION]: Finished simulation step!"
      now.set(now.get() plus config.simulationResolution)
    } catch {
      case t:Throwable =>
        t.printStackTrace()
        System.exit(1)
    }
  }

  def isRunning: Boolean = running.get()
  def queueSize(): Int = {
    log info "[SIMULATION]: Retreiving queue size..."
    tracker.get().suite.queue(now.get()).size
  }

  def incomingSuiteRate(): Double = {
    log info "[SIMULATION]: Retreiving incoming suite rate..."
    suiteInflux.get().toDouble/(config.simulationResolution.getMillis()/1000.0)
  }

  def countMachinesRunning(): Int = {
    var count = 0
    for {
      uid <- tracker.get().machine.list
      point <- tracker.get().machine.get(uid).get.life findPoint now.get()
      if point.value == MachineState.ACTIVE
    } count += 1
    count
  }

  def countMachinesRunningForEachKind(): Iterable[(UID[_], Int)] = {
    val tracker = getTracker()
    for {
      (uid, _) <- config.machineKinds
      count = tracker.machine.list.count { m_instance_uid =>
        val m = tracker.machine.get(m_instance_uid).get
        if (m.kindUID == uid) {
          val point = m.life findPoint now.get()
          point.isDefined && point.get.value == MachineState.ACTIVE
        } else {
          false
        }
      }
    } yield uid -> count
  }

  def countSuitesForEachMachineKind(): Iterable[(UID[_], Iterable[(UID[_], Int)])] = {
    val tracker = getTracker()
    val queue = tracker.suite.queue(now.get())
    for {
      (machine_uid, _) <- config.machineKinds
      suite_count_for_machine = for {
          suite_kind <- config.suiteKindsFor(machine_uid)
          number_of_suites_in_queue_of_this_kind = queue.count(tracker.suite.get(_).get.kindUID == suite_kind)
        } yield suite_kind -> number_of_suites_in_queue_of_this_kind
    } yield machine_uid -> suite_count_for_machine
  }

  def getTracker(): Tracker = tracker.get()
  def getNow(): DateTime = now.get()

  def addSuites(kinds:Iterable[String], count: Int): Unit = {
    if (kinds.isEmpty) {
      for ((kind,_) <- config.suiteKinds) {
        suiteGenerator.injectAdditionalSimulatedSuites(kind, count)
      }
    } else {
      for {
        k <- kinds
        kind = UID(k) if config.suiteKinds.contains(kind)
      }
        suiteGenerator.injectAdditionalSimulatedSuites(kind, count)
    }
    log info s"[SIMULATION]: Preparing to add $count suites."
  }

  def prepareAction(action: Action): Boolean = {
    log info s"[SIMULATION]: Preparing action for application: $action"
    this.action.set(action)
    /*
    if(!actionReady.getAndSet(true)) {
      log info s"[SIMULATION]: Preparing action for application: $action"
      this.action.set(action)
      true
    } else {
      log error "[SIMULATION]: Another action has already been prepared!"
      false
    }
    */
    true
  }

  def run(): Boolean = {
    if(!running.getAndSet(true)) {
      log info "[SIMULATION]: Starting simulation..."
      execPool.scheduleAtFixedRate(new Runnable {def run() = tick()},
        0L, THREAD_TICK_TIME, TimeUnit.MILLISECONDS)
      true
    } else {
      log error "[SIMULATION]: This simulator is already running!"
      false
    }
  }

  def stop(): Boolean = {
    if(running.getAndSet(false)) {
      log info "[SIMULATION]: Stopping simulation..."
      execPool.awaitTermination(5, TimeUnit.SECONDS)
      true
    } else {
      log error "[SIMULATION]: This simulator has already stopped!"
      false
    }
  }
}

object Simulator {
  def apply(loggable: Loggable): Simulator = new Simulator() with Logging {
    val log = loggable
  }
}
