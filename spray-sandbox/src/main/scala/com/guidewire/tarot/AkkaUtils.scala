package com.guidewire.tarot

import scala.language.implicitConversions

import akka.event.LoggingAdapter

import com.guidewire.tarot.common.Loggable

object AkkaUtils {
  @inline implicit def toLoggable(adapter:LoggingAdapter):Loggable = new Loggable {
    def debug(message: => String) = adapter.debug(message)
    def info(message: => String) = adapter.info(message)
    def warning(message: => String) = adapter.warning(message)
    def error(message: => String) = adapter.error(message)
    def error(cause:Throwable, message: => String): Unit = adapter.error(cause, message)
  }

  @inline implicit class LoggingAdapterExtensions(adapter:LoggingAdapter) {
    def toLoggable: Loggable = AkkaUtils.toLoggable(adapter)
  }
}
