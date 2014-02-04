package com.guidewire.tarot.sa

import scala.annotation.tailrec

/**
  *
  */
private[sa]
object Corun {
  /** slave.next() must never return None */
  def apply[A, B](master: Coroutine[A, B],
                  slave: Coroutine[B, A],
                  init: A): A = {
    @tailrec
    def run(x: A): A = master.next(x) match {
      case None => x
      case Some(y) => run(slave.next(y).get)
    }
    run(init)
  }
}

/**  A simple implementation of a coroutine that takes in an
  *  argument for every iteration.
  *
  *  @tparam A
  *          The type of the iteration argument.
  *  @tparam B
  *          The type of the Optional return value.
  */
private[sa]
trait Coroutine[A, B] {
  /**  Performs an iteration of the coroutine with the provided argument.
    *
    *  @param arg
    *         The argument to the iteration of the coroutine.
    *  @return Optionally returns the result of the iteration, if the
    *          coroutine is able to iterate.
    */
  def next(arg: A): Option[B]
}
