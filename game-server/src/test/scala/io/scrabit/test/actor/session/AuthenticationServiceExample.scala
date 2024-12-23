package io.scrabit.test.actor.session

import io.scrabit.actor.http.WebsocketServer
import io.scrabit.actor.session.AuthenticationService.AuthenticationServiceKey
import io.scrabit.actor.session.AuthenticationService.Login
import io.scrabit.test.actor.testkit.TestAuthenticator
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.receptionist.Receptionist
import org.apache.pekko.actor.typed.scaladsl.Behaviors

object AuthenticationServiceExample {

  @main def start(): Unit = {
    val dummyAuthenticator = TestAuthenticator()
    val root = Behaviors.setup { context =>
      val auth = context.spawnAnonymous(dummyAuthenticator)
      context.spawnAnonymous(WebsocketServer(auth, Behaviors.empty))
      Behaviors.empty
    }

    ActorSystem(root, "whalah")
  }

}
