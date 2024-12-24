package io.scrabit.actor.testkit

import org.apache.pekko.actor.typed.Behavior
import io.scrabit.actor.session.AuthenticationService.{AuthenticationServiceKey, Login}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import io.scrabit.actor.CommunicationHub.SessionCreated
import io.scrabit.actor.message.OutgoingMessage.LoginFailed
import io.scrabit.actor.session.AuthenticationService

object CFAuthenticator {
  private def isCorrectPassword(userId: String, password: String): Boolean =
    userId.contains("rabbit") || password.contains("scala")

  def apply(): Behavior[Login] = Behaviors.setup(context =>
    val logger = context.log
    Behaviors.receiveMessage { case Login(userId, password, connection, replyTo) =>
      if (isCorrectPassword(userId, password)) {
        val newSessionKey = AuthenticationService.generateSessionKey()
        replyTo ! SessionCreated(userId, newSessionKey, connection)
        logger.debug(s"User $userId passed authentication")

      } else {
        val error = "Invalid Credentials !"
        logger.debug(s"Login error: $error")
        connection ! LoginFailed(userId, error)
      }
      Behaviors.same
    }
  )
}
