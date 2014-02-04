package com.guidewire.tarot.th

import com.guidewire.tarot.common.Enum

/**
 * Describes possible machine classifications.
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
object MachineType extends Enum {
  sealed case class EnumVal private[MachineType](title: String) extends Value
  def all():Seq[EnumVal] = values

  val Unknown = EnumVal("Unknown")

  val Dev     = EnumVal("Dev")
  val Perf    = EnumVal("Perf")
  val Canary  = EnumVal("Canary")
}
