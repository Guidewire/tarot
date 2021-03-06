package com.guidewire.tarot.common

trait Loggable {
  def debug(message: => String): Unit
  def info(message: => String): Unit
  def warning(message: => String): Unit
  def error(message: => String): Unit
  def error(cause:Throwable, message: => String): Unit
}

trait Logging {
  def log:Loggable
}
