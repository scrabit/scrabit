package server

import io.scrabit.actor.message
import io.scrabit.actor.message.RoomMessage
import io.scrabit.actor.session.AuthenticationService
import io.scrabit.actor.testkit.CFAuthenticator
import io.scrabit.app.ScrabitServer
import org.apache.pekko.actor.typed.Behavior
import io.scrabit.actor.message.LobbyMessage

object Server extends ScrabitServer {

  override protected def lobbyLogic: Behavior[LobbyMessage] = Lobby.initializing

  override protected def actorSystem: String                                  = "SCRABIT-COIN-FLIP-ULTIMATE"
  override protected def authenticator: Behavior[AuthenticationService.Login] = CFAuthenticator()
}
