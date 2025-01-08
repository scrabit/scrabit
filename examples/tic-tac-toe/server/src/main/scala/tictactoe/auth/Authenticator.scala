package tictactoe.auth

import io.scrabit.actor.session.AuthenticationService.Login
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.javadsl.Behaviors
import io.scrabit.actor.session.AuthenticationService
import io.scrabit.actor.CommunicationHub.SessionCreated

object Authenticator:
  def acceptAll: Behavior[Login] =
    Behaviors.receiveMessage { case Login(userId, password, connection, replyTo) =>
      println(s"user ${userId} logged in")
      val newSessionKey = AuthenticationService.generateSessionKey()
      replyTo ! SessionCreated(userId, newSessionKey, connection)
      Behaviors.same
    }
