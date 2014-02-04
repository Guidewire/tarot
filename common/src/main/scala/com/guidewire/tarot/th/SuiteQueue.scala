package com.guidewire.tarot.th

/**
 * Describes the TH suite queue.
 */
trait SuiteQueue { self: SuiteQueueProvider =>
  val provider: SuiteQueueProvider

  //Deliberately require paren. to convey that this could have side effects.
  def size():Int = provider.size()
  def all():Stream[Suite] = provider.all()
}

trait EmptySuiteQueueProvider extends SuiteQueueProvider {
  val provider = this

  def size():Int = 0
  def all():Stream[Suite] = Stream()
}

object EmptySuiteQueue extends EmptySuiteQueueProvider {
  def apply():Stream[Suite] = all()
}