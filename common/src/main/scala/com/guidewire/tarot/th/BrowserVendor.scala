package com.guidewire.tarot.th

import com.guidewire.tarot.common.Enum

object BrowserVendor extends Enum {
  sealed case class EnumVal private[BrowserVendor](title: String) extends Value
  def all():Seq[EnumVal] = values

  val Unknown          = EnumVal("Unknown")

  val InternetExplorer = EnumVal("Internet Explorer")
  val Firefox          = EnumVal("Firefox")
  val Chrome           = EnumVal("Chrome")
}
