package com.guidewire.tarot.th

import com.guidewire.tarot.common.{Arch, Versioned, Version}

/**
 * Describes a single supported browser.
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
case class Browser(
  title:String,
  value:BrowserVendor.EnumVal,
  architecture:Arch.EnumVal,
  version:Version
) extends Versioned[BrowserVendor.EnumVal]

object Browser {
  def apply(title:String, value:BrowserVendor.EnumVal, architecture:Arch.EnumVal, version:String) = new Browser(title, value, architecture, Version(version))
}
