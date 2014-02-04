package com.guidewire.tarot.th

/**
 * Describes a TH suite.
 */
case class Suite(
  name:String,
  branch:Branch,
  product:Product,
  os:Os,
  jvm:Jvm,
  rdbms:Option[Rdbms],
  appServer:Option[AppServer],
  browser:Option[Browser]
)
