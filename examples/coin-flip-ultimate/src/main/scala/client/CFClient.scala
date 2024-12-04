package client

import client.actor.ClientLogicActor.Account
import client.actor.WebsocketClient
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.javadsl.Behaviors

import scala.annotation.tailrec

object CFClient {

  @main def play() = {

    @tailrec
    def getUserChoice: String = {
      println("Please bet: head or tail ?")
      val choice = scala.io.StdIn.readLine().trim.toLowerCase
      choice match
        case "h" | "t" | "head" | "tail" => choice
        case _ =>
          println("Invalid choice. Please enter either 'head' (h) or 'tail' (t).")
          getUserChoice
    }

    // val userChoice = getUserChoice
    
    val testAccount = Account("rabbit", "abscalar")
    val serverHost = "ws://localhost:8080"

    ActorSystem(Behaviors.setup{context =>
       context.spawnAnonymous(WebsocketClient(serverHost, testAccount))
       Behaviors.empty
    }, "client")
  }

}
