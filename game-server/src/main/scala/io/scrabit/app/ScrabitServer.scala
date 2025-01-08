package io.scrabit.app

import io.scrabit.actor.http.WebsocketServer
import io.scrabit.actor.message.RoomMessage
import io.scrabit.actor.session.AuthenticationService
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorSystem, Behavior}
import io.scrabit.actor.message.LobbyMessage

trait ScrabitServer {
  protected def authenticator: Behavior[AuthenticationService.Login]

  protected def lobbyLogic: Behavior[LobbyMessage]

  protected def actorSystem: String

  private def start(): Unit = {
    val root = Behaviors.setup { context =>
      context.spawnAnonymous(WebsocketServer(authenticator, lobbyLogic))
      Behaviors.empty
    }
    ActorSystem(root, actorSystem)
  }

  def main(args: Array[String]): Unit =
    start()

}
