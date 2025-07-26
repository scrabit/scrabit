package io.scrabit.actor.testkit

import io.scrabit.actor.CommunicationHub.SessionCreated
import io.scrabit.actor.session.AuthenticationService.Login
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors

object TestAuthenticator {
  private def isCorrectPassword(userId: String, password: String): Boolean =
    userId.contains("rabbit") || password.contains("scala")

  def apply(): Behavior[Login] = Behaviors.receiveMessage { case Login(userId, password, connection, replyTo) =>
    if (isCorrectPassword(userId, password)) {
      val newSessionKey = "secret-token-used-for-secure-communication"
      replyTo ! SessionCreated(userId, newSessionKey, connection)
      println(s"User $userId passed authentication")

    } else {
      println(s"Invalid Credentials !")
    }
    Behaviors.same
  }
}
