package com.guidewire.tarot.th

import com.guidewire.tarot.common.{Arch, Versioned, Version}

/**
 * Describes a single Java virtual machine.
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
case class Jvm(
  title:String,
  value:JvmVendor.EnumVal,
  architecture:Arch.EnumVal,
  version:Version,
  supportedSpecifications:Seq[JvmSpecification.EnumVal]
) extends Versioned[JvmVendor.EnumVal]

object Jvm {
  def apply(title:String, value:JvmVendor.EnumVal, architecture:Arch.EnumVal, version:String, supportedSpecifications:JvmSpecification.EnumVal*) = new Jvm(title, value, architecture, Version(version), supportedSpecifications)
}