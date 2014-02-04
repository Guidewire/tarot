
import play.api.GlobalSettings
import play.api.Application
import play.api.mvc._
import play.api.data._

object Global extends GlobalSettings {
  import App.View._

  override def onStart(app: Application) {
    implicit val application = app
  }

  override def onHandlerNotFound(request: RequestHeader) = {
    implicit val r:RequestHeader = request
    Results.NotFound(views.html.errors.error404()(implicitly[Option[App.View.Account.Details]], implicitly[Form[(String, String)]], implicitly[AbsoluteUri], request.flash, request))
  }
}