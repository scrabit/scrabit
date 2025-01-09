package tictactoe.main

import io.scrabit.app.ScrabitServer
import org.apache.pekko.actor.typed.Behavior
import io.scrabit.actor.message.RoomMessage
import io.scrabit.actor.session.AuthenticationService.Login
import tictactoe.auth.Authenticator
import org.apache.pekko.actor.typed.javadsl.Behaviors
import io.scrabit.actor.message.LobbyMessage
import tictactoe.logic.Lobby

object Main extends ScrabitServer {

  override protected def lobbyLogic: Behavior[LobbyMessage] = Lobby.initializing

  override protected def authenticator: Behavior[Login] = Authenticator.acceptAll

  override protected def actorSystem: String = "TTT"

}
