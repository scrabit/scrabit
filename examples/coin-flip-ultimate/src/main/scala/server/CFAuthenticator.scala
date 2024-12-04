package io.scrabit.actor.testkit

import org.apache.pekko.actor.typed.Behavior
import io.scrabit.actor.session.AuthenticationService.Login
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import io.scarabit.actor.CommunicationHub.SessionCreated
import io.scrabit.actor.message.OutgoingMessage.LoginFailed

object CFAuthenticator {
  private def isCorrectPassword(userId: String, password: String): Boolean =
    userId.contains("rabbit") || password.contains("scala")

  def apply(): Behavior[Login] = Behaviors.setup(context =>
    val logger = context.log
    Behaviors.receiveMessage { case Login(userId, password, connection, replyTo) =>
      if (isCorrectPassword(userId, password)) {
        val newSessionKey = "secret-token-used-for-secure-communication"
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
