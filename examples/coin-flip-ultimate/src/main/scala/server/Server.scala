package server

import io.scrabit.actor.http.WebsocketServer
import org.apache.pekko.actor.typed.ActorSystem
import io.scrabit.actor.testkit.CFAuthenticator
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.receptionist.Receptionist
import io.scrabit.actor.session.AuthenticationService.AuthenticationServiceKey

object Server {

  @main def start(): Unit = {
    val root = Behaviors.setup { context =>
      val authenticationService = context.spawnAnonymous(CFAuthenticator())
      context.system.receptionist ! Receptionist.Register(
        AuthenticationServiceKey,
        authenticationService
      )

      context.spawnAnonymous(WebsocketServer())
      context.spawnAnonymous(GameRoom())
      Behaviors.empty
    }
    ActorSystem(root, "SCRABIT-COIN-FLIP-ULTIMATE")
  }
}
