package com.guidewire.tarot.th

import com.guidewire.tarot.common.{OsFamily, Arch}

/**
 * Provides a versioned list of supported OSes that TH supports.
 *
 * This list would grow over time and it's best, if possible, to treat
 * it as a [[scala.collection.immutable.Stream]] of some arbitrary size.
 */
object SupportedOs {

  lazy val CENTOS:Stream[Os] = Stream(
      Os("CentOS 6.3 (x86_64)", OsFamily.Unix, OsDistribution.CentOS, OsClassification.Server, Arch.x86_64, "6.3")
    , Os("CentOS 6.4 (x86_64)", OsFamily.Unix, OsDistribution.CentOS, OsClassification.Server, Arch.x86_64, "6.4")
  )

  lazy val WINDOWS:Stream[Os] = Stream(
      Os("Windows 7 (x86_64)",              OsFamily.Windows, OsDistribution.Windows, OsClassification.Desktop, Arch.x86_64, "6.1")

    , Os("Windows Server 2003 (x86_64)",    OsFamily.Windows, OsDistribution.Windows, OsClassification.Server,  Arch.x86_64, "5.2")
    , Os("Windows Server 2008 R2 (x86_64)", OsFamily.Windows, OsDistribution.Windows, OsClassification.Server,  Arch.x86_64, "6.1")
  )

  def all():Stream[Os] = Stream(
      CENTOS
    , WINDOWS
  ).flatten
}
