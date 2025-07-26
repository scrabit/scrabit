package io.scrabit.actor.session

import io.scrabit.actor.http.WebsocketServer
import io.scrabit.actor.session.AuthenticationService.AuthenticationServiceKey
import io.scrabit.actor.session.AuthenticationService.Login
import io.scrabit.actor.testkit.TestAuthenticator
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.receptionist.Receptionist
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import io.scrabit.actor.message.LobbyMessage

object AuthenticationServiceExample {

  @main def start(): Unit = {
    val dummyAuthenticator = TestAuthenticator()
    val root = Behaviors.setup { context =>
      context.spawnAnonymous(WebsocketServer(dummyAuthenticator, Behaviors.empty))
      Behaviors.empty
    }

    ActorSystem(root, "whalah")
  }

}
