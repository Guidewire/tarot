package com.guidewire.tarot

import org.joda.time.{ReadableDateTime, ReadableInstant}
import org.joda.time.{DateTime => JodaDateTime}

/**
 */
package object common {
  val readableInstantOrdering = implicitly[Ordering[ReadableInstant]]

  implicit val ReadableDateTimeOrdering = new Ordering[ReadableDateTime] {
    def compare(a:ReadableDateTime, b:ReadableDateTime) = a.compareTo(b)
  }

  implicit val DateTimeOrdering = new Ordering[JodaDateTime] {
    def compare(a:JodaDateTime, b:JodaDateTime) = a.compareTo(b)
  }

  /**
   * Provides functionality similar to C#'s default keyword.
   * However, default is now pimped -- you can do much more with it
   * than you can C#'s.
   */
  def default[A: Default] = implicitly[Default[A]].value

  /**
   * Provides functionality similar to C#'s default keyword.
   * Use when default[A] doesn't work.
   *
   * Alternative is to use "null.asInstanceOf[A]" which will
   * accomplish the same task.
   */
  def defaultValue[A] = {
    class Temp {
      var default_value: A = _
    }
    (new Temp).default_value
  }
}