package scala.guidewire.core

import org.scalatest.junit.JUnitRunner
import org.scalatest.{SeveredStackTraces, FunSuite}
import org.junit.runner.RunWith
import org.scalatest.matchers.ShouldMatchers
import org.joda.time.DateTime

@RunWith(classOf[JUnitRunner])
class VersionTest extends FunSuite
                     with ShouldMatchers
                     with SeveredStackTraces {
  private val FULL_VERSION_NUMBER:String = "7.0.2.29_20120329.1055_458928"

  test("Parsing full version number works") {
    Version.tryParse(FULL_VERSION_NUMBER).isDefined should be (true)
  }

  test("Parsing full version number as a string works") {
    Version(FULL_VERSION_NUMBER).asString() should be (FULL_VERSION_NUMBER)
    Version("5_20120329.1055_1234").asString() should be ("5.0.0.0_20120329.1055_1234")
  }

  test("Full version number string conversion should be idempotent") {
    val TIMES:Int = 1000
    var value:String = FULL_VERSION_NUMBER
    for(i <- 0 until TIMES) {
      value = Version(value).asString()
      value should be (FULL_VERSION_NUMBER)
    }
  }

  test("Invalid numbers should not parse") {
    Version.tryParse("5.6.7.89_12345.1234_12345_123") should be (None) //Extra numbers
    Version.tryParse("5_6.7.89_12345.1234_12345")     should be (None) //First period replaced with underscore
    Version.tryParse("5.6.7.89_12345.1234_12345_")    should be (None) //Extra underscore on the end
    Version.tryParse("5.6.7.89_12345.1234_12345A")    should be (None) //Non-digit on the end
    Version.tryParse("5.6.7.89_12345.1234.12345")     should be (None) //Last underscore replaced with period
    Version.tryParse("5.6.7.89_12345.1234__12345")    should be (None) //Extra underscore in last section
    Version.tryParse("5.6.7.89_12345_1234_12345")     should be (None) //Last period replaced with underscore
    Version.tryParse("A.6.7.89_12345.1234_12345")     should be (None) //Major version replaced with non-digit
    Version.tryParse("5.6.7.89_12345.")               should be (None) //Last section removed but period still there
    Version.tryParse("5.6.7.89_12345A")               should be (None) //No last section, but a non-digit appended to the end
    Version.tryParse("5.6.7.89_")                     should be (None) //No date, etc. but left an underscore
    Version.tryParse("5.6.7_89")                      should be (None) //Period replaced with underscore
    Version.tryParse("5.6.7A")                        should be (None) //Appended a non-digit to the end
    Version.tryParse("5.6_7")                         should be (None) //Period replaced with underscore
    Version.tryParse("5.6.")                          should be (None) //Left a trailing period
    Version.tryParse("5.A")                           should be (None) //Minor version replaced with non-digit
    Version.tryParse("5.")                            should be (None) //Left a trailing period
    Version.tryParse("5_")                            should be (None) //Left a trailing underscore
    Version.tryParse("?")                             should be (None) //Simple version, non-digit for major version
    Version.tryParse(" ")                             should be (None) //Just whitespace
    Version.tryParse("")                              should be (None) //Empty string
    Version.tryParse(null)                            should be (None) //Null version
  }

  test("Composing a version instance using date parts works correctly") {
    var date:DateTime = DateTime.now()

    date = new DateTime(1981, 6, 16, 6, 4, 59)
    validate(
      date,
      Version(1, 2, 3, 4, date, 5),
      "1.2.3.4_19810616.0604_5"
    )

    date = new DateTime(700, 6, 1, 6, 4, 59)
    validate(
      date,
      Version(1, 2, 3, 4, date, 5),
      "1.2.3.4_07000601.0604_5"
    )

    val v = new CommonVersion(1, 2, 3, 4, CommonVersion.buildDatePart(0, 0, 0), CommonVersion.buildTimePart(0, 0), 5)
    v.buildYear   should equal (0)
    v.buildMonth  should equal (0)
    v.buildDay    should equal (0)
    v.buildHour   should equal (0)
    v.buildMinute should equal (0)

    v.tryAsBuildDate() should equal (None)
    v.tryAsBuildDateInMilliseconds() should equal(None)
    v.tryAsStandardBuildDate() should equal(None)

    date = new DateTime(1981, 10, 16, 23, 11, 59)
    validate(
      date,
      new CommonVersion(1, 2, 3, 4, CommonVersion.buildDatePart(1981, 10, 16), CommonVersion.buildTimePart(23, 11), 5),
      "1.2.3.4_19811016.2311_5"
    )

    evaluating {
      new CommonVersion(1, 2, 3, 4, CommonVersion.buildDatePart(-1, 0, 0), CommonVersion.buildTimePart(0, 0), 5)
    } should produce [IllegalArgumentException]

    date = new DateTime(2012, 3, 29, 10, 55, 0)
    validate(
      date,
      Version("7.0.2.29_20120329.1055_458928"),
      "7.0.2.29_20120329.1055_458928"
    )
  }

  private def stripSeconds(date:DateTime):DateTime = date.minusSeconds(date.getSecondOfMinute())

  private def validate(date:DateTime, version:Version, expectedStringVersion:String) = {
    version.buildYear     should equal (date.getYear)
    version.buildMonth    should equal (date.getMonthOfYear)
    version.buildDay      should equal (date.getDayOfMonth)
    version.buildHour     should equal (date.getHourOfDay)
    version.buildMinute   should equal (date.getMinuteOfHour)
    version.asBuildDate() should equal (stripSeconds(date))
    version.asString()    should equal (expectedStringVersion)
  }
}
