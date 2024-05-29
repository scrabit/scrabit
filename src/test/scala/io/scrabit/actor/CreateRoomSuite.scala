package io.scrabit.actor

import io.circe.Json
import io.circe.JsonObject
import io.circe.syntax._
import io.github.iltotore.iron.*
import io.scarabit.actor.CommunicationHub
import io.scarabit.actor.CommunicationHub.CreateRoom
import io.scrabit.actor.message.IncomingMessage
import io.scrabit.actor.message.IncomingMessage.RawMessage
import io.scrabit.actor.message.OutgoingMessage
import io.scrabit.actor.message.OutgoingMessage.LoginSuccess
import io.scrabit.actor.message.OutgoingMessage.UserJoinedRoom
import io.scrabit.actor.message.RoomMessage
import io.scrabit.actor.message.RoomMessage.Action
import io.scrabit.actor.message.RoomMessage.ActionType
import io.scrabit.actor.message.RoomMessage.RoomCreated
import io.scrabit.actor.session.AuthenticationService.Login
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.receptionist.Receptionist
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.http.scaladsl.model.ws.Message
import org.apache.pekko.http.scaladsl.model.ws.TextMessage
import org.scalatest.funsuite.AnyFunSuiteLike

class CreateRoomSuite extends ScalaTestWithActorTestKit, AnyFunSuiteLike:
  private val testUserId = "rabbit"
  private val sessionKey = "session-key-abc"

  case class Hello(userId: String, message: String) extends Action {

    override def payload: Option[JsonObject] = None

    override def tpe: ActionType = 10
  }

  case class NiceToMeetYa(userId: String) extends OutgoingMessage {

    override def recipients: List[String] = List(userId)

    override def data: Json = s"Hi user $userId. Nice to meet ya!".asJson

    override def tpe: Int = 3
  }

  class Game(comHub: ActorRef[CommunicationHub.Message]) {
    val behavior: Behavior[RoomMessage] = Behaviors.receiveMessagePartial { case Hello(userId, message) =>
      comHub ! NiceToMeetYa(userId)
      Behaviors.same
    }
  }

  test("Create new room") {
    val probe = testKit.createTestProbe[RoomMessage]()
    val authenticator = testKit.spawn(
      Behaviors.empty[Login]
    ) // Authenticator is not involved in Room creation
    val commHub = testKit.spawn(CommunicationHub.create(authenticator))
    commHub ! CreateRoom(testUserId, Behaviors.ignore[RoomMessage], probe.ref)
    probe.expectMessageType[RoomCreated]
  }

  test("Login - Create Room - JoinRoom - Send Room Request") {
    val authenticator =
      testKit.spawn(Behaviors.receiveMessage[Login] { case Login(userId, password, connection, replyTo) =>
        if (userId.contains("rabbit") || password.contains("scala")) {
          replyTo ! CommunicationHub.SessionCreated(
            userId,
            sessionKey,
            connection
          )
        }
        Behaviors.same
      })

    val outgoingMesssageProbe = testKit.createTestProbe[OutgoingMessage]()
    val connection            = outgoingMesssageProbe.ref

    val roomMessageProbe = testKit.createTestProbe[RoomMessage]()

    val commHub = testKit.spawn(CommunicationHub.create(authenticator))
    commHub ! RawMessage(s"LOGIN-$testUserId/password", connection)
    outgoingMesssageProbe.expectMessage(LoginSuccess(testUserId, sessionKey))

    testKit.spawn(Behaviors.setup { context =>
      commHub ! CreateRoom(testUserId, new Game(commHub).behavior, context.self)
      Behaviors.receiveMessagePartial { case roomCreated: RoomCreated =>
        roomMessageProbe.ref ! roomCreated
        roomCreated.ref ! Hello(testUserId, "Life is just a video game")
        Behaviors.same
      }
    })

    roomMessageProbe.expectMessageType[RoomCreated]
    outgoingMesssageProbe.expectMessageType[UserJoinedRoom]
    outgoingMesssageProbe.expectMessage(NiceToMeetYa(testUserId))
  }
