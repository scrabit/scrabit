package server.main

import io.scrabit.actor.message
import io.scrabit.actor.session.AuthenticationService
import io.scrabit.app.ScrabitServer
import org.apache.pekko.actor.typed.Behavior

object TicTacToeServer extends ScrabitServer{
  override protected def authenticator: Behavior[AuthenticationService.Login] = ???

  override protected def gameLogic: Behavior[message.RoomMessage] = ???

  override protected def actorSystem: String = ???
}
