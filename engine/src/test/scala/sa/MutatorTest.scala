package com.guidewire.tarot.sa

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers._
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith

import org.joda.time.Duration
import com.guidewire.tarot.{Action, Config, UID, MachineKind}

@RunWith(classOf[JUnitRunner])
class MutatorTest extends FunSuite
                  with ShouldMatchers {

  val config = new Config(
    Map(
      UID("machine-kind-0") -> MachineKind(new Duration(50L), new Duration(50L), 1, 0.5),
      UID("machine-kind-1") -> MachineKind(new Duration(50L), new Duration(50L), 1, 0.5),
      UID("machine-kind-2") -> MachineKind(new Duration(50L), new Duration(50L), 1, 0.5),
      UID("machine-kind-3") -> MachineKind(new Duration(50L), new Duration(50L), 1, 0.5)
    ),
    Map(),
    simulationResolution=new Duration(100L),
    averageSuiteArrivalRate=1.0/25.0,
    randomSeed=12345L
  )
  val saConfig = SimulatedAnnealingConfig(
    simulationDepth=10,
    suiteWaitPrice=50.0,
    SAEvaluateRepeats=10,
    SAMutateDensity=2,
    SAMutateSize=1,
    SAMutateProbability=0.1,
    SATemperatureCutoff=0.02,
    SATemperatureDecay=0.900,
    notifyProgressListeners=(progress: Double)=>{}
  )

  test("Mutate only when divisible by SAMutateDensity") {
    val mutator = new Mutator(config, saConfig)

    val zeroAction = new Action(config.machineKinds map (e => e._1 -> 0))
    var actionPath: Seq[Action] = 0 until saConfig.simulationDepth map (_ => zeroAction)

    for (_ <- 0 until 10) {
      for ((action, index) <- actionPath.zipWithIndex)
        if(!(index % saConfig.SAMutateDensity == 0))
          action should be(zeroAction)
      actionPath = mutator.scramble(actionPath)
    }
  }
}
