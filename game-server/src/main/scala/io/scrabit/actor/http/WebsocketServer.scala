package io.scrabit.actor.http

import io.scrabit.actor.CommunicationHub
import io.scrabit.actor.message.*
import io.scrabit.actor.session.AuthenticationService
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Behavior}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.ws.{Message, TextMessage}
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.stream.OverflowStrategy
import org.apache.pekko.stream.scaladsl.{Flow, Keep, Sink, Source}
import org.apache.pekko.stream.typed.scaladsl.ActorSource

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.*
import scala.util.{Failure, Success}

object WebsocketServer {

  private def route(commHub: ActorRef[IncomingMessage], log: String => Unit)(using ActorSystem[?]) =
    path("") {
      val (actorRef, publisher) = ActorSource
        .actorRef[OutgoingMessage](
          completionMatcher = PartialFunction.empty,
          bufferSize = 200,
          failureMatcher = PartialFunction.empty,
          overflowStrategy = OverflowStrategy.fail
        )
        .toMat(Sink.asPublisher[OutgoingMessage](fanout = false))(Keep.both)
        .run()

      val inSink = Sink.foreach[Message] {
        case TextMessage.Strict(text) =>
          commHub ! IncomingMessage.RawMessage(text, actorRef)
        case unhandledMessage @ _ =>
          log(s"Received unhandled message $unhandledMessage")

      }

      handleWebSocketMessages(
        Flow.fromSinkAndSource(inSink, Source.fromPublisher[OutgoingMessage](publisher).map(_.toWsMessage))
      )
    }

  def apply(authenticator: Behavior[AuthenticationService.Login], lobbyLogic: Behavior[LobbyMessage]): Behavior[Nothing] = Behaviors.setup {
    context =>
      val port                                                = 8080
      implicit val system: ActorSystem[Nothing]               = context.system
      implicit val executionContext: ExecutionContextExecutor = context.executionContext
      val authenticationService                               = context.spawnAnonymous(authenticator)
      val lobby                                               = context.spawnAnonymous(lobbyLogic)

      val communicationHub = context.spawn(CommunicationHub.create(authenticationService, lobby), "communication-hub")

      Http()
        .newServerAt("localhost", port)
        .bind(route(communicationHub, context.log.debug))
        .andThen {
          case Success(binding) => binding.addToCoordinatedShutdown(5.seconds)
          case Failure(exception) =>
            context.log.error(s"Failed To start Websocket Server : ${exception.getMessage()}")
        }
      Behaviors.empty
  }
}
