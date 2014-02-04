package com.guidewire.tarot.th

import com.guidewire.tarot.common.Enum

object MachineStatus extends Enum {
  sealed case class EnumVal private[MachineStatus](title: String) extends Value
  def all():Seq[EnumVal] = values

  val Unknown   = EnumVal("Unknown")

  val Available = EnumVal("Available")
  val Disabled  = EnumVal("Disabled")
  val Failed    = EnumVal("Failed")
  val Imaging   = EnumVal("Imaging")
}
