package com.guidewire.tarot.th

import com.guidewire.tarot.common.Arch

/**
 * Describes a JVM that an appserver supports.
 */
case class AppServerJvm(
  vendor:JvmVendor.EnumVal,
  architecture:Arch.EnumVal,
  specifications:Seq[JvmSpecification.EnumVal]
)

object AppServerJvm {
}