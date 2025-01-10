package io.scrabit.test.actor

import io.circe.Json
import io.circe.syntax.*
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.numeric.*
import io.scrabit.actor.CommunicationHub
import io.scrabit.actor.message.IncomingMessage.Request
import io.scrabit.actor.message.OutgoingMessage
import io.scrabit.actor.message.OutgoingMessage.LoginSuccess
import io.scrabit.actor.message.OutgoingMessage.UserJoinedRoom
import io.scrabit.actor.message.RoomMessage
import io.scrabit.actor.message.RoomMessage.Action
import io.scrabit.actor.message.RoomMessage.ActionType
import io.scrabit.actor.message.RoomMessage.RoomCreated
import io.scrabit.actor.message.RoomMessage.UserJoined
import io.scrabit.actor.session.AuthenticationService.Login
import io.scrabit.test.actor.message.TestRequest
import io.scrabit.test.actor.testkit.TestAuthenticator
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.scalatest.funsuite.AnyFunSuiteLike
import io.scrabit.actor.message.LobbyMessage
import io.scrabit.actor.CommunicationHub.CreateRoomGame

class CreateRoomSuite extends ScalaTestWithActorTestKit, AnyFunSuiteLike:
  private val testUserId = "rabbit"
  private val sessionKey = "secret-token-used-for-secure-communication"

  object Hello {
    val ACTION_TYPE: ActionType = 10
    def apply(userId: String, message: String): Request =
      TestRequest.action(
        userId,
        ACTION_TYPE,
        Json
          .obj(
            "message" -> message.asJson
          )
          .asObject
      )

    def unapply(action: Action): Option[(String, String)] =
      if action.tpe != ACTION_TYPE then None
      else
        for {
          messageJson <- action.payload.flatMap(_.apply("message"))
          message     <- messageJson.asString
        } yield (action.userId, message)

  }

  case class NiceToMeetYa(recipient: String) extends OutgoingMessage {

    override def data: Json = s"Hi user $recipient. Nice to meet ya!".asJson

    override def tpe: Int = 10
  }

  object Lobby {

    def apply(comHub: ActorRef[CommunicationHub.Message], probe: ActorRef[LobbyMessage | RoomMessage]): Behavior[LobbyMessage] =
      Behaviors.receiveMessage {
        case msg @ LobbyMessage.CreateRoomRequest(userId, roomName) =>
          probe ! msg
          comHub ! CreateRoomGame(userId, roomName, new Game(comHub, probe).behavior)
          Behaviors.same
        case LobbyMessage.Init(_) =>
          // avoid fowarding Init message to probe
          Behaviors.same

        case msg @ _ =>
          probe ! msg
          Behaviors.same
      }

  }

  class Game(comHub: ActorRef[CommunicationHub.Message], probe: ActorRef[RoomMessage]) {
    val behavior: Behavior[RoomMessage] = Behaviors.setup(context =>
      Behaviors.receiveMessage {
        case Hello(userId, message) =>
          comHub ! NiceToMeetYa(userId)
          Behaviors.same

        case UserJoined(userId) =>
          context.log.debug(s"User $userId joined Game")
          Behaviors.same

        case msg @ _ =>
          probe ! msg
          Behaviors.same
      }
    )
  }

  test("Login --> Create Room --> JoinRoom --> Room Request/Response") {
    val authenticator        = testKit.spawn(TestAuthenticator())
    val outgoingMessageProbe = testKit.createTestProbe[OutgoingMessage]()

    val roomMessageProbe = testKit.createTestProbe[LobbyMessage | RoomMessage]()

    val commHub: ActorRef[CommunicationHub.Message] = testKit.spawn(Behaviors.setup { context =>
      val lobby = testKit.spawn(Lobby(context.self, roomMessageProbe.ref))
      CommunicationHub.create(authenticator, lobby)
    })

    commHub ! TestRequest.login(testUserId, "pscalassword", outgoingMessageProbe.ref)

    outgoingMessageProbe.expectMessage(LoginSuccess(testUserId, sessionKey))

    commHub ! TestRequest.createRoom(testUserId, "Havefun")

    roomMessageProbe.expectMessageType[LobbyMessage.LoggedIn]

    roomMessageProbe.expectMessageType[LobbyMessage.CreateRoomRequest]

    roomMessageProbe.expectMessageType[LobbyMessage.GameRoomCreated]

    outgoingMessageProbe.expectMessageType[UserJoinedRoom]

    commHub ! Hello(testUserId, "Wow")

    outgoingMessageProbe.expectMessage(NiceToMeetYa(testUserId))
  }
