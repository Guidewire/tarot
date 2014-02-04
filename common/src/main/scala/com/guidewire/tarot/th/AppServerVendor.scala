package com.guidewire.tarot.th

import com.guidewire.tarot.common.Enum

/**
 * Describes app server vendors that TH supports.
 */
object AppServerVendor extends Enum {
  sealed case class EnumVal private[AppServerVendor](title: String) extends Value
  def all():Seq[EnumVal] = values

  val Unknown   = EnumVal("Unknown")

  val Jetty     = EnumVal("Jetty")
  val Tomcat    = EnumVal("Tomcat")
  val JBoss     = EnumVal("RedHat JBoss")
  val WebSphere = EnumVal("IBM WebSphere")
  val WebLogic  = EnumVal("Oracle WebLogic")
}
