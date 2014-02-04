package com.guidewire.tarot.th

/**
 * Describes a TH product. e.g.: CC (ClaimCenter), PC (PolicyCenter), etc.
 */
case class Product(
  name:String,
  shortName:String,
  longName:String
) {
  def this(name:String) = this(name, name, name)
}
