package com.guidewire.tarot

import scala.collection.{mutable, Seq}
import scala.math._
import scala.util.Random
import com.guidewire.tarot.chart.{ValueSeries, Chart}

class Annealing1(private[this] val num:Int) {
  case class World()
  case class Solution(index:Int, value:Double)

  private[this] val universe_range = 0 to 1000000
  private[this] val universe = generateRandomValues(num, universe_range)

  def generateRandomValues(n:Int, range:Range):mutable.ArrayBuffer[Double] = {
    val arr = new mutable.ArrayBuffer[Double](n)
    val start = range.start.toDouble
    val end = range.end.toDouble
    val value_range = abs(end - start)

    for(i <- 0 until n) {
      arr += (start + (Random.nextDouble() * value_range))
    }

    arr
  }

  def randomlySelectInRange(range:Range):Int = {
    val start = range.start
    val end = range.end
    val value_range = abs(end - start)
    val rand = Random.nextInt(value_range)

    start + rand
  }

  def randomSolution(): Solution = {
    //Pick a value randomly in my universe.
    val randomly_selected_index = randomlySelectInRange(0 to num - 1)

    Solution(randomly_selected_index, universe(randomly_selected_index))
  }

  def mutate(solution:Solution):Solution = {
    val randomly_selected_index_within_close_range = min(num - 1, max(0, solution.index + randomlySelectInRange(-10 to 10)))

    Solution(randomly_selected_index_within_close_range, universe(randomly_selected_index_within_close_range))
  }

  def cost(solution:Solution, world:World):Double = {
    //Cost of something is the value itself for this simple attempt.
    solution.value
  }

  def attempt1(): Chart[Double, Double] = {
    var best:Solution = randomSolution()
    var here:Solution = best
    var there:Solution = null

    var best_score:Double = cost(here, World())
    var current:Double = best_score
    var local:Double = 0.0D

    var t = 1.0D
    while(t >= Math.sqrt(0.0004D)) {
      there = mutate(here)
      local = cost(there, World())
      if (local >= current || Random.nextDouble() <= t) {
        here = there
        current = local

        if (local > best_score) {
          best = there
          best_score = local
        }
      }
      t *= Math.sqrt(0.9988D)
    }

    //Generate points that reflect the values
    val indexed_universe = universe.zipWithIndex.map { t =>
      val (value, index) = t
      (index.toDouble, value)
    }
    Chart("Simulated Annealing #1")(ValueSeries("Data")(indexed_universe:_*), ValueSeries("Solution")((best.index, 0), (best.index, best.value)))
  }
}

object Annealing1 {
  def apply() = new Annealing1(num = 250).attempt1()
}
