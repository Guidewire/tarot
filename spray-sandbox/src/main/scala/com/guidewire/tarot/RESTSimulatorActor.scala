package com.guidewire.tarot

import spray.json._

object RESTSimulator {
  case class AddSuiteRequest(kinds:Iterable[String], suites: Int)
  case class AddSuiteResponse(success: Boolean, message: String = "")

  object Serializer extends DefaultJsonProtocol {
    implicit val add_suite_request_format = jsonFormat2(AddSuiteRequest)
    implicit val add_suite_response_format = jsonFormat2(AddSuiteResponse)
  }
}
