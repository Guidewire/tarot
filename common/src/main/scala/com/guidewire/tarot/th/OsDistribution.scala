package com.guidewire.tarot.th

import com.guidewire.tarot.common.Enum

/**
  * Describes possible OS distributions.
  */
object OsDistribution extends Enum {
  sealed case class EnumVal private[OsDistribution](title: String) extends Value
  def all():Seq[EnumVal] = values

  val Unknown = EnumVal("Unknown")

  val CentOS  = EnumVal("CentOS")
  val Windows = EnumVal("Windows")
}