package tictactoe.main

import io.scrabit.app.ScrabitServer
import org.apache.pekko.actor.typed.Behavior
import io.scrabit.actor.message.RoomMessage
import io.scrabit.actor.session.AuthenticationService.Login
import tictactoe.auth.Authenticator
import org.apache.pekko.actor.typed.javadsl.Behaviors

object Main extends ScrabitServer {

  override protected def authenticator: Behavior[Login] = Authenticator.acceptAll

  override protected def gameLogic: Behavior[RoomMessage] = Behaviors.empty

  override protected def actorSystem: String = "TTT"

}
