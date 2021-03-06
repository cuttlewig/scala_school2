package com.twitter.scaffold

import akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import akka.io.IO
import spray.can.Http
import spray.http.StatusCodes.{ BadRequest, NoContent }
import spray.routing.HttpService

class Scaffold extends Actor with HttpService {

  // TODO #6: there should be many console actors supervised by Scaffold, one per user session
  val console = context.actorOf(Console.props, "console")

  /* HttpService */
  override val actorRefFactory = context

  /* Actor */
  override def receive = runRoute(assetsRoute ~ markdownRoute ~ consoleRoute)

  /* Scaffold */
  private[this] val assetsRoute =
    pathPrefix("assets") {
      getFromResourceDirectory("META-INF/resources/webjars") ~
      getFromResourceDirectory("assets")
    }

  private[this] val markdownRoute = 
    get {
      import spray.httpx.TwirlSupport._
      path(Slash) {
        complete { html.index() }
      } ~
      path(Rest) {
        Document.render(_) match {
          case Some(html) => complete { html }
          case None => reject
        }
      }
    }

  private[this] val consoleRoute =
    post {
      import com.twitter.spray._
      entity(as[String]) {
        Console.Interpret(_) ~> console ~> {
          case Console.Success(message) => complete { message }
          case Console.Failure(message) => respondWithStatus(BadRequest) { complete { message } }
        }
      }
    } ~
    delete {
      complete {
        console ! Console.Reset
        NoContent
      }
    }

}

object Scaffold extends App {
  implicit val system = akka.actor.ActorSystem("scaffold-system")

  val props = Props[Scaffold]
  val scaffold = system.actorOf(props, "scaffold")

  IO(Http) ! Http.Bind(
    listener  = scaffold,
    interface = "localhost",
    port      = 8080
  )
}
