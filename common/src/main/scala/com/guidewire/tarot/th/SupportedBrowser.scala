package com.guidewire.tarot.th

import com.guidewire.tarot.common.Arch

/**
 * Provides a versioned list of supported browsers that TH supports.
 *
 * This list would grow over time and it's best, if possible, to treat
 * it as a [[scala.collection.immutable.Stream]] of some arbitrary size.
 */
object SupportedBrowser {
  lazy val INTERNET_EXPLORER:Stream[Browser] = Stream(
      Browser("Internet Explorer 6.0", BrowserVendor.InternetExplorer, Arch.x86,    "6.0")

    , Browser("Internet Explorer 7.0", BrowserVendor.InternetExplorer, Arch.x86,    "7.0")
    , Browser("Internet Explorer 7.0", BrowserVendor.InternetExplorer, Arch.x86_64, "7.0")

    , Browser("Internet Explorer 8.0", BrowserVendor.InternetExplorer, Arch.x86,    "8.0")
    , Browser("Internet Explorer 8.0", BrowserVendor.InternetExplorer, Arch.x86_64, "8.0")

    , Browser("Internet Explorer 9.0", BrowserVendor.InternetExplorer, Arch.x86,    "9.0")
    , Browser("Internet Explorer 9.0", BrowserVendor.InternetExplorer, Arch.x86_64, "9.0")
  )

  lazy val FIREFOX:Stream[Browser] = Stream(
      Browser("Firefox 3.6",  BrowserVendor.Firefox, Arch.x86, "3.6")
    , Browser("Firefox 12.0", BrowserVendor.Firefox, Arch.x86, "12.0")
    , Browser("Firefox 19.0", BrowserVendor.Firefox, Arch.x86, "19.0")
    , Browser("Firefox 20.0", BrowserVendor.Firefox, Arch.x86, "20.0")
  )

  lazy val CHROME:Stream[Browser] = Stream(
      Browser("Chrome 25.0", BrowserVendor.Chrome, Arch.x86, "25.0")
  )

  def all():Stream[Browser] = Stream(
      INTERNET_EXPLORER
    , FIREFOX
    , CHROME
  ).flatten
}
