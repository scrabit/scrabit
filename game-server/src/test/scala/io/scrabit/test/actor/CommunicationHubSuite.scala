package io.scrabit.test.actor

import io.scrabit.actor.CommunicationHub
import io.scrabit.actor.message.IncomingMessage.RawMessage
import io.scrabit.actor.message.OutgoingMessage
import io.scrabit.actor.message.OutgoingMessage.LoginSuccess
import io.scrabit.test.actor.message.TestRequest
import io.scrabit.test.actor.testkit.TestAuthenticator
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.apache.pekko.actor.testkit.typed.scaladsl.TestProbe
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.javadsl.Behaviors
import org.scalatest.funsuite.AnyFunSuiteLike
import io.github.iltotore.iron.constraint.numeric.*
import io.github.iltotore.iron.*
import io.scrabit.actor.message.LobbyMessage

class CommunicationHubSuite extends ScalaTestWithActorTestKit, AnyFunSuiteLike:

  private val authenticator    = TestAuthenticator()
  private val authenticatorRef = testKit.spawn(authenticator)

  private def assertLogin(username: String, password: String)(
    expectation: (TestProbe[OutgoingMessage], TestProbe[LobbyMessage]) => Unit
  ): Unit = {
    val outgoingMessaggeProbe = testKit.createTestProbe[OutgoingMessage]()
    val lobbyMessageProbe     = testKit.createTestProbe[LobbyMessage]()
    val connection            = outgoingMessaggeProbe.ref
    val lobby = testKit.spawn(
      Behaviors.receiveMessage[LobbyMessage] {
        case LobbyMessage.Init(commHub) => // avoid fowarding Init message to probe
          Behaviors.same
        case msg @ _ =>
          lobbyMessageProbe.ref ! msg
          Behaviors.same
      }
    )
    val commHub = testKit.spawn(CommunicationHub.create(authenticatorRef, lobby))
    commHub ! TestRequest.login(username, password, connection)
    expectation(outgoingMessaggeProbe, lobbyMessageProbe)
  }

  test("Login flow: success") {
    assertLogin("rabbit1", "youneverknow") { case (outgoingMessaggeProbe, lobbyMessageProbe) =>
      outgoingMessaggeProbe.expectMessage(LoginSuccess("rabbit1", "secret-token-used-for-secure-communication"))
      lobbyMessageProbe.expectMessage(LobbyMessage.LoggedIn("rabbit1"))
    }

    assertLogin("littlepig", "ilovescala") { (probe, lobbyMessageProbe) =>
      probe.expectMessage(LoginSuccess("littlepig", "secret-token-used-for-secure-communication"))
      lobbyMessageProbe.expectMessage(LobbyMessage.LoggedIn("littlepig"))
    }
  }

  test("Login flow: failure") {
    assertLogin("wolf", "openthedoornow") { (probe, lobbyMessageProbe) =>
      probe.expectNoMessage()
      lobbyMessageProbe.expectNoMessage()
    }
  }

  test("Send message with invalid session key") { // FIXME: this test case is not good enough because there won't be any response even if the session key is valid
    val probe             = testKit.createTestProbe[OutgoingMessage]()
    val lobbyMessageProbe = testKit.createTestProbe[LobbyMessage]()

    val commHub    = testKit.spawn(CommunicationHub.create(authenticatorRef, lobbyMessageProbe.ref))
    val connection = probe.ref
    commHub ! TestRequest.sessionMessage("rabbit1", 10, "invalid-session-key", connection)
    probe.expectNoMessage()
  }
