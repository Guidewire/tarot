package com.guidewire.tarot.th

import com.guidewire.tarot.common.{OsFamily, Versioned, Version, Arch}

/**
 * Describes possible OSes.
 */
case class Os(
  title:String,
  value:OsFamily.EnumVal,
  distribution:OsDistribution.EnumVal,
  classification:OsClassification.EnumVal,
  architecture:Arch.EnumVal,
  version:Version
) extends Versioned[OsFamily.EnumVal]

object Os {
  def apply(title:String, value:OsFamily.EnumVal, distribution:OsDistribution.EnumVal, classification:OsClassification.EnumVal, architecture:Arch.EnumVal, version:String) =
    new Os(title, value, distribution, classification, architecture, Version(version))
}