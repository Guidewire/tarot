package com.guidewire.tarot.th

import com.guidewire.tarot.common.{Versioned, Version}

/**
 * Describes a single relational database management system.
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
case class Rdbms(
  title:String,
  value:RdbmsVendor.EnumVal,
  version:Version
) extends Versioned[RdbmsVendor.EnumVal]

object Rdbms {
  def apply(title:String, value:RdbmsVendor.EnumVal, version:String) = new Rdbms(title, value, Version(version))
}
