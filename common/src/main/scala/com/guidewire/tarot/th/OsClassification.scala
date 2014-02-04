package com.guidewire.tarot.th

import com.guidewire.tarot.common.Enum

/**
  * Describes possible OS classifications.
  */
object OsClassification extends Enum {
  sealed case class EnumVal private[OsClassification](title: String) extends Value
  def all():Seq[EnumVal] = values

  val Unknown = EnumVal("Unknown")

  val Server  = EnumVal("Server")
  val Desktop = EnumVal("Desktop")
}