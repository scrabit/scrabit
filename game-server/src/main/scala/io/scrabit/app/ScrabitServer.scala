package io.scrabit.app

import io.scrabit.actor.http.WebsocketServer
import io.scrabit.actor.message.RoomMessage
import io.scrabit.actor.session.AuthenticationService
import io.scrabit.actor.session.AuthenticationService.AuthenticationServiceKey
import org.apache.pekko.actor.typed.receptionist.Receptionist
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorSystem, Behavior}

trait ScrabitServer {
  protected def authenticator: Behavior[AuthenticationService.Login]
  
  protected def gameLogic: Behavior[RoomMessage] 
  
  protected def actorSystem: String
  
  private def start(): Unit = {
    val root = Behaviors.setup { context =>
      val authenticationService = context.spawnAnonymous(authenticator)
      context.spawnAnonymous(WebsocketServer(authenticationService, gameLogic))
      Behaviors.empty
    }
    ActorSystem(root, actorSystem)
  }

  def main(args: Array[String]): Unit = {
    start()
  }
  
}
