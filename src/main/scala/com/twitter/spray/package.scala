package com.twitter

package object spray {

  import akka.actor.{ Actor, ActorContext, ActorRef, Props }
  import _root_.spray.routing.{ RequestContext, Route }

  class Continuation(run: PartialFunction[Any, Route], ctx: RequestContext) extends Actor {
    def receive = run andThen {
      case route =>
        try route(ctx)
        catch { case util.control.NonFatal(e) => ctx.failWith(e) }
        finally context.stop(self)
    }
  }
  object Continuation {
    def props(run: PartialFunction[Any, Route], ctx: RequestContext): Props =
      Props(classOf[Continuation], run, ctx)
  }

  class AndThen(message: Any, ref: ActorRef, factory: ActorContext) {
    def ~>(run: PartialFunction[Any, Route])(ctx: RequestContext): Unit = {
      val continuation = factory actorOf Continuation.props(run, ctx)
      ref.tell(message, continuation)
    }
  }

  implicit class MessageOps(message: Any)(implicit factory: ActorContext) {
    def ~>(ref: ActorRef): AndThen = new AndThen(message, ref, factory)
  }

}
