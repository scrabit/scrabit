package client.actor

import client.actor.ClientLogicActor.Account
import client.message.{IncomingMessage, Request}
import io.circe.parser.*
import org.apache.pekko.Done
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.model.ws.*
import org.apache.pekko.http.scaladsl.{Http, model}
import org.apache.pekko.stream.OverflowStrategy
import org.apache.pekko.stream.scaladsl.*
import org.apache.pekko.stream.typed.scaladsl.ActorSource

import scala.concurrent.{ExecutionContext, Future}
object WebsocketClient {

  def apply(uri: String, account: Account): Behavior[Request] = Behaviors.setup { context =>
    val logicRef = context.spawnAnonymous(ClientLogicActor(context.self, account))
    val forwardSink: Sink[Message, Future[Done]] =
      Sink.foreach {
        case message: TextMessage.Strict =>
          val rawMessage = for {
            json   <- parse(message.text).toOption
            obj    <- json.asObject
            userId <- obj("userId").flatMap(_.asString)
            tpe    <- obj("tpe").flatMap(_.asNumber).flatMap(_.toInt)
            data   <- obj("payload").flatMap(_.asObject)
          } yield IncomingMessage(userId, tpe, data)
          rawMessage.foreach(msg => logicRef ! msg)
        case _ =>
        // ignore other message types
      }

    val sendingSource = ActorSource
      .actorRef[Message](
        completionMatcher = PartialFunction.empty,   // Define the condition to complete the stream
        failureMatcher = PartialFunction.empty,      // Define the condition to fail the stream
        bufferSize = 10,                             // Buffer size for backpressure
        overflowStrategy = OverflowStrategy.dropHead // Overflow strategy
      )

    val flow: Flow[Message, Message, ActorRef[Message]] =
      Flow.fromSinkAndSourceMat(forwardSink, sendingSource)(Keep.right)

    given ActorSystem = context.system.classicSystem
    val (upgradeResponse, publish) = Http().singleWebSocketRequest(WebSocketRequest(uri), flow)
    given ExecutionContext         = context.executionContext
    val connected = upgradeResponse.map { upgrade =>
      if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
        logicRef ! ClientLogicActor.Connected
      }
    }
    Behaviors.receiveMessage{ request =>
        publish ! request.toWsMessage
        Behaviors.same
    }
  }

}
