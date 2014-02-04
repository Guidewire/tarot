package com.guidewire.tarot.th

/**
 * Abstracts the source of the suite queue.
 */
trait SuiteQueueProvider {
  def size():Int
  def all():Stream[Suite]
}
