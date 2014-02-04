package com.guidewire.tarot.th

import com.guidewire.tarot.common.Enum

/**
 * Describes possible JVM implementations.
 */
object JvmVendor extends Enum {
  sealed case class EnumVal private[JvmVendor](title: String) extends Value
  def all():Seq[EnumVal] = values

  val Unknown = EnumVal("Unknown")

  val Hotspot = EnumVal("Hotspot")
  val IBM     = EnumVal("IBM")
  val JRockit = EnumVal("JRockit")
}
