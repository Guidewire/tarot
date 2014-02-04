package scala.guidewire.core

import org.joda.time.{ReadableDateTime, DateTime => JodaDateTime}
import java.util.Date
import scala.util.matching.Regex

/**
 * Works with version numbers of the following format:
 *     major.minor.maintenance.buildnumber_yyyyMMdd[date].hhmm[time]_revision
 *
 * For example:
 *     1.0.0.15_20131252.999_123456
 *
 * Implementers of this interface should be considered immutable and safe for concurrent access.
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
trait Version {
  def major:Int
  def minor:Int
  def maintenance:Int

  def buildNumber:Int
  def date:Int
  def time:Int
  def revision:Int

  def buildYear:Int
  def buildMonth:Int
  def buildDay:Int
  def buildHour:Int
  def buildMinute:Int

  def asBuildDate():JodaDateTime
  def asBuildDateInMilliseconds():Long
  def asStandardBuildDate(): Date
  def tryAsBuildDate():Option[JodaDateTime]
  def tryAsBuildDateInMilliseconds():Option[Long]
  def tryAsStandardBuildDate():Option[Date]

  def asString():String
  def asString(builder:StringBuilder):StringBuilder
}

/**
 * Works with version numbers as specified by [[com.guidewire.tarot.common.Version]]
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
object CommonVersion {
  import StringUtil._

  private val REGEX_VERSION_STRING_FULL                                     = """(\d+)\.(\d+)\.(\d+)\.(\d+)_(\d+)\.(\d+)_(\d+)""".r
  private val REGEX_VERSION_STRING_MAJOR_MINOR_MAINTENANCE_BUILDNUMBER_DATE = """(\d+)\.(\d+)\.(\d+)\.(\d+)_(\d+)""".r
  private val REGEX_VERSION_STRING_MAJOR_MINOR_MAINTENANCE_BUILDNUMBER      = """(\d+)\.(\d+)\.(\d+)\.(\d+)""".r
  private val REGEX_VERSION_STRING_MAJOR_MINOR_MAINTENANCE                  = """(\d+)\.(\d+)\.(\d+)""".r
  private val REGEX_VERSION_STRING_MAJOR_MINOR                              = """(\d+)\.(\d+)""".r
  private val REGEX_VERSION_STRING_MAJOR                                    = """(\d+)""".r

  private val REGEX_ALT_VERSION_STRING_MAJOR_DATE_TIME_REVISION             = """(\d+)_(\d+)\.(\d+)_(\d+)""".r

  private val REGEX_VERSION_STRING_MATCH_LIST_IN_ORDER:Seq[Regex]           = Seq(
      REGEX_VERSION_STRING_FULL
    , REGEX_VERSION_STRING_MAJOR_MINOR_MAINTENANCE_BUILDNUMBER_DATE
    , REGEX_VERSION_STRING_MAJOR_MINOR_MAINTENANCE_BUILDNUMBER
    , REGEX_VERSION_STRING_MAJOR_MINOR_MAINTENANCE
    , REGEX_VERSION_STRING_MAJOR_MINOR
    , REGEX_VERSION_STRING_MAJOR
  )

  private val REGEX_ALT_VERSION_STRING_MATCH_LIST_IN_ORDER:Seq[Regex]       = Seq(
      REGEX_ALT_VERSION_STRING_MAJOR_DATE_TIME_REVISION
  )

  private val YEAR_MULTIPLIER   = 10000
  private val MONTH_MULTIPLIER  = 100
  private val DAY_MULTIPLIER    = 1

  private val HOUR_MULTIPLIER   = 100
  private val MINUTE_MULTIPLIER = 1

  def buildDatePart(date:ReadableDateTime):Int = buildDatePart(date.getYear, date.getMonthOfYear, date.getDayOfMonth)
  def buildTimePart(date:ReadableDateTime):Int = buildTimePart(date.getHourOfDay, date.getMinuteOfHour)
  def buildDatePart(year:Int, month:Int, day:Int):Int = year * YEAR_MULTIPLIER + month * MONTH_MULTIPLIER + day * DAY_MULTIPLIER
  def buildTimePart(hour:Int, minute:Int):Int = hour * HOUR_MULTIPLIER + minute * MINUTE_MULTIPLIER

  def parse(version:String):CommonVersion = {
    val parsed = tryParse(version)
    require(parsed.isDefined, s"Invalid version number: $version. Expected format: #.#.#.##_yyyyMMdd.hhmm_####")
    parsed.get
  }

  def tryParse(version:String):Option[CommonVersion] = {
    if (version.isNullOrEmpty) {
      None
    } else {
      for {
        p <- REGEX_VERSION_STRING_MATCH_LIST_IN_ORDER
        m = p.pattern.matcher(version) if m.matches()
        count = m.groupCount()
      } {
        count match {
          case 7 => return Some(new CommonVersion(parseInt(m.group(1)), parseInt(m.group(2)), parseInt(m.group(3)), parseInt(m.group(4)), parseInt(m.group(5)), parseInt(m.group(6)), parseInt(m.group(7))))
          case 5 => return Some(new CommonVersion(parseInt(m.group(1)), parseInt(m.group(2)), parseInt(m.group(3)), parseInt(m.group(4)), parseInt(m.group(5)), 0, 0))
          case 4 => return Some(new CommonVersion(parseInt(m.group(1)), parseInt(m.group(2)), parseInt(m.group(3)), parseInt(m.group(4)), 0, 0, 0))
          case 3 => return Some(new CommonVersion(parseInt(m.group(1)), parseInt(m.group(2)), parseInt(m.group(3)), 0, 0, 0, 0))
          case 2 => return Some(new CommonVersion(parseInt(m.group(1)), parseInt(m.group(2)), 0, 0, 0, 0, 0))
          case 1 => return Some(new CommonVersion(parseInt(m.group(1)), 0, 0, 0, 0, 0, 0))
        }
      }

      for {
        p <- REGEX_ALT_VERSION_STRING_MATCH_LIST_IN_ORDER
        m = p.pattern.matcher(version) if m.matches()
        count = m.groupCount()
      } {
        count match {
          case 4 => return Some(new CommonVersion(parseInt(m.group(1)), 0, 0, 0, parseInt(m.group(2)), parseInt(m.group(3)), parseInt(m.group(4))))
        }
      }

      None
    }
  }

  def isValid(version:String) = tryParse(version).isDefined

  private def validateNumbers(values:Int*) = for (value <- values) {
    require(value >= 0, "Every part of the version must be a positive integer")
  }

  private def validateBuildDate(version:CommonVersion) =
    try {
      version.asBuildDate()
    } catch {
      case t:Throwable => throw new IllegalArgumentException("Invalid build date", t)
    }


  def parseInt(value:String):Int =
    try {
      value.toInt
    } catch {
      case _:Throwable => -1
    }
}

/**
 * Works with version numbers as specified by [[com.guidewire.tarot.common.Version]]
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
class CommonVersion(val major:Int, val minor:Int, val maintenance:Int, val buildNumber:Int, val date:Int, val time:Int, val revision:Int) extends Version {
  import CommonVersion._

  validateNumbers(major, minor, maintenance, buildNumber, date, time, revision)

  def buildYear:Int = date / YEAR_MULTIPLIER
  def buildMonth:Int = (date - (buildYear * YEAR_MULTIPLIER)) / MONTH_MULTIPLIER
  def buildDay:Int = (date - buildYear * YEAR_MULTIPLIER - buildMonth * MONTH_MULTIPLIER) / MINUTE_MULTIPLIER
  def buildHour:Int = time / HOUR_MULTIPLIER
  def buildMinute:Int = (time - buildHour * HOUR_MULTIPLIER) / MINUTE_MULTIPLIER
  def asBuildDate():JodaDateTime = new JodaDateTime(buildYear, buildMonth, buildDay, buildHour, buildMinute, 0)
  def asBuildDateInMilliseconds():Long = asBuildDate.getMillis
  def asStandardBuildDate():Date = asBuildDate.toDate

  def tryAsBuildDate():Option[JodaDateTime] =
    try {
      Some(new JodaDateTime(buildYear, buildMonth, buildDay, buildHour, buildMinute, 0))
    } catch {
      case _:Throwable => None
    }

  def tryAsBuildDateInMilliseconds():Option[Long] =
    try {
      Some(asBuildDateInMilliseconds())
    } catch {
      case _:Throwable => None
    }

  def tryAsStandardBuildDate():Option[Date] =
    try {
      Some(asStandardBuildDate())
    } catch {
      case _:Throwable => None
    }

  def asString(builder:StringBuilder):StringBuilder = {
    val b = if (builder eq null) new StringBuilder(128) else builder
    b.append(asString())
  }

  def asString():String = "%d.%d.%d.%d_%08d.%04d_%d".format(major, minor, maintenance, buildNumber, date, time, revision) //major.minor.maintenance.buildnumber_date.time_revision

  override def toString:String = asString()

  override def equals(o:Any):Boolean = o match {
    case v:Version => buildNumber == v.buildNumber && date == v.date && maintenance == v.maintenance && major == v.major && minor == v.minor && revision == v.revision && time == v.time
    case _ => false
  }

  override def hashCode:Int = {
    var result = major
    result = 31 * result + minor
    result = 31 * result + maintenance
    result = 31 * result + buildNumber
    result = 31 * result + date
    result = 31 * result + time
    result = 31 * result + revision
    result
  }
}

/**
 * Works with version numbers as specified by [[com.guidewire.tarot.common.Version]]
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
object Version {
  def apply(major:Int, minor:Int, maintenance:Int, buildNumber:Int, date:ReadableDateTime, revision:Int) = {
    if (date eq null) {
      new CommonVersion(major, minor, maintenance, buildNumber, 0, 0, revision)
    } else {
      new CommonVersion(major, minor, maintenance, buildNumber, CommonVersion.buildDatePart(date), CommonVersion.buildTimePart(date), revision)
    }
  }
  def apply(v: String):Version = CommonVersion.parse(v)
  def tryParse(v:String):Option[Version] = CommonVersion.tryParse(v)
}
