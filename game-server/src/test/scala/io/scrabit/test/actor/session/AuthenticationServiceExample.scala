package io.scrabit.test.actor.session

import org.apache.pekko.actor.typed.Behavior
import io.scrabit.actor.session.AuthenticationService.Login
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import io.scarabit.actor.CommunicationHub.SessionCreated
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.receptionist.Receptionist
import io.scrabit.actor.session.AuthenticationService.AuthenticationServiceKey
import io.scarabit.actor.CommunicationHub
import io.scrabit.actor.http.WebsocketServer
import io.scrabit.test.actor.testkit.TestAuthenticator

object AuthenticationServiceExample {

  @main def start(): Unit = {
    val dummyAuthenticator = TestAuthenticator()
    val root = Behaviors.setup { context =>
      val authenticationService = context.spawnAnonymous(dummyAuthenticator)
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
