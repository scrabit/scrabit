package client.main

import client.actor.ClientLogicActor.Account
import client.actor.WebsocketClient
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.javadsl.Behaviors

object CFClient {

  @main def play(): Unit = {
    val testAccount = Account("rabbit", "ilovescala")
    val serverHost = "ws://localhost:8080"

    ActorSystem(Behaviors.setup{context =>
       context.spawnAnonymous(WebsocketClient(serverHost, testAccount))
       Behaviors.empty
    }, "client")
  }

}
