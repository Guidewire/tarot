package com.guidewire.tarot.th

/**
 * Describes a TH branch.
 */
case class Branch(
  name:String,
  shortName:String,
  longName:String,
  vcsPath:String,
  locked:Boolean
) {
  def this(name:String, vcsPath:String) = this(name, name, name, vcsPath, false)
  def this(name:String, vcsPath:String, locked:Boolean) = this(name, name, name, vcsPath, locked)
}
