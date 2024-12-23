package io.scrabit.test.actor.testkit

import org.apache.pekko.actor.typed.Behavior
import io.scrabit.actor.session.AuthenticationService.Login
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import io.scrabit.actor.CommunicationHub.SessionCreated

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
