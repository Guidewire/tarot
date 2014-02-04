package controllers

import play.api._
import play.api.mvc._
import play.api.Play.current
import play.api.data._
import play.api.data.Forms._
import play.i18n.{Messages, Lang}

object Application extends Controller {
  import App._
  import App.View._

  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      form_with_errors => {
        BadRequest(views.html.login()(implicitly[Option[App.View.Account.Details]], form_with_errors, implicitly[AbsoluteUri], implicitly[Flash], implicitly[RequestHeader]))
      },
      values => values match {
        case(account, _) =>
          Redirect(routes.Authenticated.engine)
            .withSession("account" -> account)
            .flashing("success" -> "Login successful")
      }
    )
  }

  def index = NotLoggedIn {
    implicit request =>
      Ok(views.html.index())
  } {
    implicit request => implicit user =>
      Redirect(routes.Authenticated.engine)
  }

  def login = Action { implicit request =>
    Ok(views.html.login())
  }

  def logout = Action {
    Redirect(routes.Application.index).withNewSession.flashing(
      "success" -> "Successfully logged out"
    )
  }

  def predictive = Action { implicit request =>
    Ok(views.html.predictive())
  }

  def team = Action { implicit request =>
    Ok(views.html.team())
  }

  def news = Action { implicit request =>
    Ok(views.html.news())
  }

  def support = Action { implicit request =>
    Ok(views.html.support())
  }

  def terms = Action { implicit request =>
    Ok(views.html.terms())
  }
}