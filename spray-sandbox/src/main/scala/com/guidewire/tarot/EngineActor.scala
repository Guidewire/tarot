package com.guidewire.tarot

import akka.actor.{Props, ActorSystem, ActorRef, Actor}
import scala.collection.mutable
import spray.util.{LoggingContext, SprayActorLogging}
import akka.event.LoggingAdapter
import org.joda.time.DateTime
import com.guidewire.tarot.common.Loggable

trait EngineProvider {
  def engine: ActorRef
}

object EngineActor {
  import Engine._

  case class RegisterProgressListener(listener: ActorRef)
  case class ProgressEvent(progress: ProgressUpdate)
  case class UnregisterProgressListener(listener: ActorRef)

  case class RegisterNewBestListener(listener: ActorRef)
  case class NewBestResultsEvent(results: NewBestResults)
  case class UnregisterFinishedListener(listener: ActorRef)

  case class RegisterFinishedListener(listener: ActorRef)
  case class FinishedEvent(results: Results)
  case class UnregisterNewBestListener(listener: ActorRef)

  case class ResultsResponse(results: Results)
  case class Update(update: EngineUpdate)

  case class ConfigurationResponse(config: Config)

  case object RequestResult
  case class RunPass(now: DateTime)

  case object RequestConfiguration

  private case class ActorProgressListener(listener:ActorRef) extends Listener[ProgressUpdate] {
    def callBack(progress: ProgressUpdate): Unit =
      listener ! ProgressEvent(progress)
  }

  private case class ActorNewBestListener(listener:ActorRef) extends Listener[NewBestResults] {
    def callBack(results: NewBestResults): Unit =
      listener ! NewBestResultsEvent(results)
  }

  private case class ActorFinishedListener(listener:ActorRef) extends Listener[Results] {
    def callBack(results: Results): Unit =
      listener ! FinishedEvent(results)
  }

  def apply(name:String)(implicit system:ActorSystem): ActorRef =
    system.actorOf(Props[EngineActor], name)
}

class EngineActor extends Actor with SprayActorLogging {
  import Engine._
  import EngineActor._
  import AkkaUtils._

  private[this] val engine = Engine(log)
  private[this] val progressListeners = mutable.LinkedHashSet[ActorRef]()
  private[this] val finishedListeners = mutable.LinkedHashSet[ActorRef]()
  private[this] val newBestListeners = mutable.LinkedHashSet[ActorRef]()

  def receive = {
    case RegisterProgressListener(listener) =>
      progressListeners add listener
      engine.progressListeners.add(ActorProgressListener(listener))

    case RegisterNewBestListener(listener) =>
      newBestListeners add listener
      engine.newBestListeners.add(ActorNewBestListener(listener))

    case RegisterFinishedListener(listener) =>
      finishedListeners add listener
      engine.finishedListeners.add(ActorFinishedListener(listener))

    case UnregisterProgressListener(listener) =>
      progressListeners remove listener
      engine.progressListeners.remove(ActorProgressListener(listener))

    case UnregisterNewBestListener(listener) =>
      newBestListeners remove listener
      engine.newBestListeners.remove(ActorNewBestListener(listener))

    case UnregisterFinishedListener(listener) =>
      finishedListeners remove listener
      engine.finishedListeners.remove(ActorFinishedListener(listener))

    case RunPass(now: DateTime) =>
      engine.run(now)

    case Update(update: EngineUpdate) =>
      engine.update(update)

    case RequestResult =>
      sender ! ResultsResponse(engine.currentResults)

    case RequestConfiguration =>
      sender ! ConfigurationResponse(engine.config)
  }

}
