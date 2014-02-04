package com.guidewire.tarot.th

import com.guidewire.tarot.common.Enum

/**
 * Describes possible JVM specifications.
 */
object JvmSpecification extends Enum {
  sealed case class EnumVal private[JvmSpecification](title: String) extends Value
  def all():Seq[EnumVal] = values

  val Unknown = EnumVal("Unknown")

  val v1_4    = EnumVal("v1.4")
  val v1_5    = EnumVal("v1.5")
  val v1_6    = EnumVal("v1.6")
  val v1_7    = EnumVal("v1.7")
  val v1_8    = EnumVal("v1.8")
}
