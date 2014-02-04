package com.guidewire.tarot.th

import com.guidewire.tarot.common.Arch

/**
 * Provides a versioned list of supported app servers that TH supports.
 *
 * This list would grow over time and it's best, if possible, to treat
 * it as a [[scala.collection.immutable.Stream]] of some arbitrary size.
 */
object SupportedAppServer {

  lazy val JETTY:Stream[AppServer] = Stream(
      AppServer("Jetty 6", AppServerVendor.Jetty, "6.0",
        AppServerJvm(JvmVendor.Hotspot, Arch.x86,    JvmSpecification.all),
        AppServerJvm(JvmVendor.Hotspot, Arch.x86_64, JvmSpecification.all)
      )
  )

  lazy val TOMCAT:Stream[AppServer] = Stream(
      AppServer("Tomcat 5", AppServerVendor.Tomcat, "5.0",
        AppServerJvm(JvmVendor.Hotspot, Arch.x86,    JvmSpecification.all),
        AppServerJvm(JvmVendor.Hotspot, Arch.x86_64, JvmSpecification.all)
      )
    , AppServer("Tomcat 6", AppServerVendor.Tomcat, "6.0",
        AppServerJvm(JvmVendor.Hotspot, Arch.x86,    JvmSpecification.all),
        AppServerJvm(JvmVendor.Hotspot, Arch.x86_64, JvmSpecification.all)
      )
    , AppServer("Tomcat 7", AppServerVendor.Tomcat, "7.0",
        AppServerJvm(JvmVendor.Hotspot, Arch.x86_64, JvmSpecification.all)
      )
  )

  lazy val JBOSS:Stream[AppServer] = Stream(
      AppServer("JBoss 5.0", AppServerVendor.JBoss, "5.0",
        AppServerJvm(JvmVendor.Hotspot, Arch.x86,    JvmSpecification.all),
        AppServerJvm(JvmVendor.Hotspot, Arch.x86_64, JvmSpecification.all)
      )
    , AppServer("JBoss 5.1", AppServerVendor.JBoss, "5.1",
        AppServerJvm(JvmVendor.Hotspot, Arch.x86,    JvmSpecification.all),
        AppServerJvm(JvmVendor.Hotspot, Arch.x86_64, JvmSpecification.all)
      )
    , AppServer("JBoss 6.0", AppServerVendor.JBoss, "6.0",
        AppServerJvm(JvmVendor.Hotspot, Arch.x86_64, JvmSpecification.all)
      )
  )

  lazy val WEBSPHERE:Stream[AppServer] = Stream(
      AppServer("WebSphere 6.0", AppServerVendor.WebSphere, "6.0",
        AppServerJvm(JvmVendor.IBM, Arch.x86,    JvmSpecification.all),
        AppServerJvm(JvmVendor.IBM, Arch.x86_64, JvmSpecification.all)
      )
    , AppServer("WebSphere 7.0", AppServerVendor.WebSphere, "7.0",
        AppServerJvm(JvmVendor.IBM, Arch.x86,    JvmSpecification.all),
        AppServerJvm(JvmVendor.IBM, Arch.x86_64, JvmSpecification.all)
      )
    , AppServer("WebSphere 8.0", AppServerVendor.WebSphere, "8.0",
        AppServerJvm(JvmVendor.IBM, Arch.x86_64, JvmSpecification.all)
      )
    , AppServer("WebSphere 8.5", AppServerVendor.WebSphere, "8.5",
        AppServerJvm(JvmVendor.IBM, Arch.x86_64, JvmSpecification.all)
      )
  )

  lazy val WEBLOGIC:Stream[AppServer] = Stream(
      AppServer("WebLogic 10.0", AppServerVendor.WebLogic, "10.0",
        AppServerJvm(JvmVendor.Hotspot, Arch.x86,    JvmSpecification.all),
        AppServerJvm(JvmVendor.Hotspot, Arch.x86_64, JvmSpecification.all)
      )
    , AppServer("WebLogic 10.3", AppServerVendor.WebLogic, "10.3",
        AppServerJvm(JvmVendor.Hotspot, Arch.x86,    JvmSpecification.all),
        AppServerJvm(JvmVendor.Hotspot, Arch.x86_64, JvmSpecification.all)
      )
    , AppServer("WebLogic 10.3.5", AppServerVendor.WebLogic, "10.3.5",
        AppServerJvm(JvmVendor.Hotspot, Arch.x86,    JvmSpecification.all),
        AppServerJvm(JvmVendor.Hotspot, Arch.x86_64, JvmSpecification.all)
      )
    , AppServer("WebLogic 12.0", AppServerVendor.WebLogic, "12.0",
        AppServerJvm(JvmVendor.Hotspot, Arch.x86_64, JvmSpecification.all)
      )
  )

  def all():Stream[AppServer] = Stream(
      JETTY
    , TOMCAT
    , JBOSS
    , WEBSPHERE
    , WEBLOGIC
  ).flatten
}
