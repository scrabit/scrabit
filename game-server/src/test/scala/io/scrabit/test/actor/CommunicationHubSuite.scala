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

class CommunicationHubSuite extends ScalaTestWithActorTestKit, AnyFunSuiteLike:

  private val authenticator       = TestAuthenticator()
  private val authenticatorRef    = testKit.spawn(authenticator)
  private val communicationHubRef = testKit.spawn(CommunicationHub.create(authenticatorRef, Behaviors.empty))

  private def assertLogin(commHub: ActorRef[RawMessage], username: String, password: String)(
    expectation: TestProbe[OutgoingMessage] => Unit
  ): Unit = {
    val probe      = testKit.createTestProbe[OutgoingMessage]()
    val connection = probe.ref
    commHub ! TestRequest.login(username, password, connection)
    expectation(probe)
  }

  test("Login flow: success") {
    assertLogin(communicationHubRef, "rabbit1", "youneverknow") { probe =>
      probe.expectMessage(LoginSuccess("rabbit1", "secret-token-used-for-secure-communication"))
    }

    assertLogin(communicationHubRef, "littlepig", "ilovescala") { probe =>
      probe.expectMessage(LoginSuccess("littlepig", "secret-token-used-for-secure-communication"))
    }
  }

  test("Login flow: failure") {
    assertLogin(communicationHubRef, "wolf", "openthedoornow") { probe =>
      probe.expectNoMessage()
    }
  }
