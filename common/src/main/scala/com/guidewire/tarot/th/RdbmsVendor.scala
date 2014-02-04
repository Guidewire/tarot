package com.guidewire.tarot.th

import com.guidewire.tarot.common.Enum

object RdbmsVendor extends Enum {
  sealed case class EnumVal private[RdbmsVendor](title: String) extends Value
  def all():Seq[EnumVal] = values

  val Unknown   = EnumVal("Unknown")

  val H2        = EnumVal("H2")
  val SqlServer = EnumVal("Sql Server")
  val Oracle    = EnumVal("Oracle")
  val DB2       = EnumVal("DB2")
}
