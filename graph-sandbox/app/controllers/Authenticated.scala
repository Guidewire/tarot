package controllers

import play.api.mvc._
import App.View

object Authenticated extends Controller {
  import App._
  import App.View._

  def engine = Secure { implicit request => implicit account =>
    Ok(views.html.authenticated.engine())
  }

  def details = Secure { implicit request => implicit account =>
    Ok(views.html.authenticated.details())
  }

  def log = Secure { implicit request => implicit account =>
    Ok(views.html.authenticated.log())
  }

}
