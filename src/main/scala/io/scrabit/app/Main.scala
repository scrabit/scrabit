package io.scrabit.app

import org.apache.pekko.actor.typed.ActorSystem
import io.scrabit.actor.http.WebsocketServer

object Main {
  def main(args: Array[String]): Unit = {
    ActorSystem(WebsocketServer(), "SCRABIT-GAME-SERVER")
  }
}
