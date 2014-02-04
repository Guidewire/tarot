package com.guidewire.tarot.th

import com.guidewire.tarot.common.{Versioned, Version}

/**
 * Describes app servers that TH supports.
 */
case class AppServer(
  title:String,
  value:AppServerVendor.EnumVal,
  version:Version,
  supportedJvms:Stream[AppServerJvm]
) extends Versioned[AppServerVendor.EnumVal]

object AppServer {
  def apply(title:String, value:AppServerVendor.EnumVal, version:String, supportedJvms:AppServerJvm*) =
    new AppServer(title, value, Version(version), supportedJvms.toStream)
}
