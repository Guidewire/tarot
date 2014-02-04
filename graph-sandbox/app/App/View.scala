package App

import play.api.libs.json.Json
import play.api.mvc._
import play.api.data._
import scala.collection._
import scala.math._
import scala.util.{Try, Random}
import com.guidewire.tarot.metrics.MetricsLogParser
import com.guidewire.tarot.chart._
import com.guidewire.tarot.Annealing1
import controllers.routes
import play.api.libs.iteratee.{Input, Done}

object View {
  import model._

  val applicationTitle:String = "Project Tarot"

  def allDoubleLineCharts():Seq[DoubleLineChart] = {
    def randomValues(n:Int = 100, xAxisRange:Range = 0 to 100, yAxisRange:Range = 0 to 10000):Seq[(Double, Double)] = {
      var pairs = mutable.MutableList[(Double, Double)]()
      val x_end = xAxisRange.end.toDouble
      val x_step = abs(x_end - xAxisRange.start.toDouble) / n.toDouble
      var x_curr = xAxisRange.start.toDouble
      var i = 0
      val y_start = yAxisRange.start.toDouble
      val y_end = yAxisRange.end.toDouble
      val y_range = abs(y_start - y_end)

      while(x_curr <= x_end) {
        pairs += ((x_curr, y_start + (Random.nextDouble() * y_range)))

        i += 1
        x_curr += x_step
      }

      pairs
    }

    val charts = Seq(
      Annealing1(),
      Chart("Random chart #1")(ValueSeries("Random data")(randomValues():_*))
    )

    //Assign a "unique" id to the chart.
    charts.zipWithIndex.map { t =>
      val (chart, index) = t
      DoubleLineChart("value_chart_" + (index + 1), 0.0D, 0.0D, chart)
    }
  }

  def allHistograms():Seq[DateTimeHistogramChart] = {
    val app = play.api.Play.current
    val files = Seq("tarot-queue-2013-6-21.log", "tarot-queue-2013-6-26.log")

    val charts =
      for {
        file_name <- files
        f = app.getExistingFile("examples/" + file_name)
        file <- f

        log = MetricsLogParser(file)
        log_pairs = log.producePairs()

        chart = Chart(file_name)(HistogramSeries(file_name, 15.minutes)(log_pairs:_*))
      }
        yield chart

    //Assign a "unique" id to the chart.
    charts.zipWithIndex.map { t =>
      val (chart, index) = t
      DateTimeHistogramChart("histogram_chart_" + (index + 1), chart)
    }
  }

  def prettyPrint(value: String) = {
    try {
      Json.prettyPrint(Json.parse(value))
    } catch {
      case _: Throwable => value
    }
  }

  trait AbsoluteUri {
    def uri:String
    override def toString:String = uri
  }

  implicit def request2AbsoluteUri(implicit request:RequestHeader):AbsoluteUri = new AbsoluteUri {
    def uri = controllers.routes.Application.index().absoluteURL(false)
  }

  object Authenticate {
    def apply(account:String, password:String) = model.User.authenticate(account, password)
  }

  object Account {
    case class Details(account:String, displayName:String, email:String)

    def apply(account:String):Option[Details] = model.User.lookup(account)
  }

  implicit def request2AccountDetails(implicit request:RequestHeader): Option[Account.Details] = {
    val a = request.session.get("account")
    if (a.isDefined) {
      User.lookup(a.get)
    } else {
      None
    }
  }

  object Secure {
    def apply(fnLoggedIn: RequestHeader => Option[Account.Details] => Result):Action[AnyContent] =
      LoggedIn(fnLoggedIn)()
  }

  object NotLoggedIn {
    def apply(fnNotLoggedIn: RequestHeader => Result = LoggedIn.unauthorized)(fnLoggedIn: RequestHeader => Option[Account.Details] => Result):Action[AnyContent] =
      LoggedIn(fnLoggedIn)(fnNotLoggedIn)
  }

  object LoggedIn {
    def unauthorized(request: RequestHeader):Result =
      Results.Redirect(routes.Application.login).flashing("error" -> "Please sign in first")

    def apply(fnLoggedIn: RequestHeader => Option[Account.Details] => Result)(fnNotLoggedIn: RequestHeader => Result = unauthorized):Action[AnyContent] = {
      Action { implicit request =>
        val account = request2AccountDetails(request)
        if (account.isDefined) {
          fnLoggedIn(request)(account)
        } else {
          fnNotLoggedIn(request)
        }
      }
    }
  }

  implicit lazy val loginForm:Form[(String, String)] = controllers.Login.loginForm
}
