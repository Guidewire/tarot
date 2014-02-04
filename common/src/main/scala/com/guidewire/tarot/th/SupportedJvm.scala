package com.guidewire.tarot.th

import com.guidewire.tarot.common.Arch

/**
 * Provides a versioned list of supported JVMs that TH supports.
 *
 * This list would grow over time and it's best, if possible, to treat
 * it as a [[scala.collection.immutable.Stream]] of some arbitrary size.
 */
object SupportedJvm {
  import JvmSpecification._

  lazy val HOTSPOT:Stream[Jvm] = Stream(
      Jvm("Oracle Hotspot v1.4 (x86)",    JvmVendor.Hotspot, Arch.x86,    "1.4", v1_4)
    , Jvm("Oracle Hotspot v1.5 (x86)",    JvmVendor.Hotspot, Arch.x86,    "1.5", v1_4, v1_5)
    , Jvm("Oracle Hotspot v1.5 (x86_64)", JvmVendor.Hotspot, Arch.x86_64, "1.5", v1_4, v1_5)
    , Jvm("Oracle Hotspot v1.6 (x86)",    JvmVendor.Hotspot, Arch.x86,    "1.6", v1_4, v1_5, v1_6)
    , Jvm("Oracle Hotspot v1.6 (x86_64)", JvmVendor.Hotspot, Arch.x86_64, "1.6", v1_4, v1_5, v1_6)
    , Jvm("Oracle Hotspot v1.7 (x86_64)", JvmVendor.Hotspot, Arch.x86_64, "1.7", v1_4, v1_5, v1_6, v1_7)
    , Jvm("Oracle Hotspot v1.8 (x86_64)", JvmVendor.Hotspot, Arch.x86_64, "1.8", v1_4, v1_5, v1_6, v1_7, v1_8)
  )

  lazy val IBM:Stream[Jvm] = Stream(
      Jvm("IBM J9 VM (x86)",    JvmVendor.IBM, Arch.x86,    "2.6", v1_4, v1_5, v1_6)
    , Jvm("IBM J9 VM (x86_64)", JvmVendor.IBM, Arch.x86_64, "2.6", v1_4, v1_5, v1_6, v1_7)
  )

  lazy val JRockit:Stream[Jvm] = Stream(
      Jvm("JRockit R28 (x86)",    JvmVendor.JRockit, Arch.x86,    "28.0", v1_4, v1_5, v1_6, v1_7)
    , Jvm("JRockit R28 (x86_64)", JvmVendor.JRockit, Arch.x86_64, "28.0", v1_4, v1_5, v1_6, v1_7)
  )

  def all():Stream[Jvm] = Stream(
      HOTSPOT
    , IBM
    , JRockit
  ).flatten
}
