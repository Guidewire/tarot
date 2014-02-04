package com.guidewire.tarot

import akka.actor._
import spray.routing._
import spray.http._
import spray.httpx.encoding._
import spray.httpx.SprayJsonSupport._

object RESTActor {
  def apply(dataFeedProvider: ActorRef, engine: ActorRef, simulator: ActorRef, name:String)(implicit system: ActorSystem) =
    system.actorOf(Props(new RESTActor(dataFeedProvider, engine, simulator)), name)
}

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class RESTActor(val dataFeedProvider: ActorRef, val engine: ActorRef, val simulator: ActorRef) extends HttpServiceActor
  with RESTService
  with EngineProvider
  with SimulatorProvider
  with DataFeedProvider {

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(route)
}

// this trait defines our service behavior independently from the service actor
trait RESTService extends HttpService { this: EngineProvider with SimulatorProvider with DataFeedProvider =>
  import MediaTypes._
  import HttpHeaders._
  import CacheDirectives._

  implicit def execution_context = actorRefFactory.dispatcher

  def eventStream(fn: RequestContext => Unit): Route =
    respondWithHeader(`Cache-Control`(`no-cache`)) {
      respondWithHeader(`Connection`("Keep-Alive")) {
        respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {
          respondWithMediaType(MediaType.custom("text/event-stream")){
            get { ctx =>
              fn(ctx)
            }
          }
        }
      }
    }

  def crossSiteHeaders(methods:String) = respondWithHeaders(
    RawHeader("Access-Control-Allow-Origin", "*"),
    RawHeader("Access-Control-Allow-Headers", "content-type, accept, origin"),
    RawHeader("Access-Control-Allow-Methods", s"OPTIONS, $methods")
  )

  val default_route = path("") {
    get {
      reject
    }
  }

  val log_route = path("log") {
    eventStream { ctx =>
      actorRefFactory.actorOf(Props(new RESTLogActor(ctx)))
    }
  }

  val engine_route = path("engine") {
    eventStream { ctx =>
      actorRefFactory.actorOf(Props(new RESTEngineActor(ctx, dataFeedProvider, engine)))
    }
  }

  val configuration_route = path("configuration") {
    respondWithHeaders(`Cache-Control`(`no-cache`), RawHeader("Access-Control-Allow-Origin", "*")) {
      respondWithMediaType(`application/json`) {
        get { ctx =>
          actorRefFactory.actorOf(Props(new RESTConfigurationActor(ctx, engine)))
        }
      }
    }
  }

  val simulator_route = path("simulator") {
    options {
      crossSiteHeaders("POST") {
        complete(StatusCodes.OK)
      }
    } ~
    post {
      crossSiteHeaders("POST") {
        respondWithHeaders(`Cache-Control`(`no-cache`)) {
          respondWithMediaType(`application/json`) {
            import RESTSimulator._
            import RESTSimulator.Serializer._

            entity(as[AddSuiteRequest]) { add_suite_request =>
              //Add a cap to the number of suites.
              val count = add_suite_request.suites match {
                case x if x < 0 => 0
                case x if x > 300 => 300
                case x => x
              }
              simulator ! SimulatorActor.AddSuites(add_suite_request.kinds, count)
              complete(AddSuiteResponse(true, s"${add_suite_request.suites} suite(s) added successfully"))
            }
          }
        }
      }
    }
  }

  val route = default_route ~ engine_route ~ configuration_route ~ simulator_route ~ log_route
}
