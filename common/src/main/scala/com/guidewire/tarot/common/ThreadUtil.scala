package com.guidewire.tarot.common

import java.util.concurrent.Callable

object ThreadUtil {
  def asCallable(fn: => Unit): Callable[Unit] = new Callable[Unit] {
    def call(): Unit = fn
  }
}
