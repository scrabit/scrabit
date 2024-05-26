package io.scrabit.actor.session

import org.apache.pekko.actor.typed.Behavior
import io.scrabit.actor.session.AuthenticationService.Login
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import io.scarabit.actor.CommunicationHub.SessionCreated
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.receptionist.Receptionist
import io.scrabit.actor.session.AuthenticationService.AuthenticationServiceKey
import io.scarabit.actor.CommunicationHub
import io.scrabit.actor.http.WebsocketServer

object AuthenticationServiceExample {

  object DummyAuthenticator {

    private def isCorrectPassword(userId: String, password: String): Boolean =
      userId.contains("rabbit") || password.contains("scala")

    def apply(): Behavior[Login] = Behaviors.receiveMessage { case Login(userId, password, connection, replyTo) =>
      if (isCorrectPassword(userId, password)) {
        val newSessionKey = "secret-token-used-for-secure-communication"
        replyTo ! SessionCreated(userId, newSessionKey, connection)
        println(s"User $userId passed authentication")

      } else {
        println(s"Invalid Credentials !")
      }
      Behaviors.same
    }

  }

  @main
  def start(): Unit = {
    val root = Behaviors.setup { context =>
      val authenticationService = context.spawnAnonymous(DummyAuthenticator())
      context.system.receptionist ! Receptionist.Register(
        AuthenticationServiceKey,
        authenticationService
      ) // Register Authentication Service with Receptionist
      context.spawnAnonymous(WebsocketServer())
      Behaviors.empty
    }

    ActorSystem(root, "whalah")
  }

}
