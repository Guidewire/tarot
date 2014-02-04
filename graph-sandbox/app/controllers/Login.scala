package controllers

import play.api.data._
import play.api.data.Forms._

import model._

object Login {
  implicit val loginForm = Form(tuple(
        "account"  -> nonEmptyText
      , "password" -> nonEmptyText
    ) verifying (
      "Invalid account or password",
      _ match {
        case (account, password) => User.authenticate(account, password).isDefined
      }
    )
  )
}
