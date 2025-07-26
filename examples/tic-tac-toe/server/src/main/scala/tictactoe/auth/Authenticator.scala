package tictactoe.auth

import io.scrabit.actor.session.AuthenticationService.Login
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import io.scrabit.actor.session.AuthenticationService
import io.scrabit.actor.CommunicationHub.SessionCreated

object Authenticator:
  def acceptAll: Behavior[Login] =
    Behaviors.setup(context =>
      Behaviors.receiveMessage { case Login(userId, password, connection, replyTo) =>
        // val newSessionKey = AuthenticationService.generateSessionKey()
        val newSessionKey = s"testkey-$userId"
        context.log.info(s"user ${userId} logged in - generated session key $newSessionKey")
        replyTo ! SessionCreated(userId, newSessionKey, connection)
        Behaviors.same
      }
    )
