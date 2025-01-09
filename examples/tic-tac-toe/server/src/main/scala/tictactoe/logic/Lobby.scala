package tictactoe.logic

import org.apache.pekko.actor.typed.Behavior
import io.scrabit.actor.message.LobbyMessage
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.ActorRef
import io.scrabit.actor.CommunicationHub
import tictactoe.logic.GameLogic
import io.scrabit.actor.message.RoomMessage
import io.scrabit.actor.message.OutgoingMessage
import io.circe.Json
import io.circe.syntax.*

/*FIXME: Duplicate code in all lobbies */
object Lobby:

  case class GameRoom(id: Int, owner: String, ref: ActorRef[RoomMessage]) // TOOD: room might have isPlaying state

  case class LobbyState(rooms: List[GameRoom])

  case class LobbyInfo(userId: String, rooms: List[GameRoom]) extends OutgoingMessage {
    override val tpe: Int = 12

    override def data: Json = Json.obj(
      "rooms" ->
        Json.arr(rooms.map(room => Json.obj("id" -> room.id.asJson, "owner" -> room.owner.asJson))*)
    )
  }

  val initializing: Behavior[LobbyMessage] = Behaviors.receiveMessagePartial { case LobbyMessage.Init(commHub) =>
    println("Lobby initialized")
    active(commHub, LobbyState(Nil))
  }

  private def active(commHub: ActorRef[CommunicationHub.Message], state: LobbyState): Behavior[LobbyMessage] = Behaviors.receiveMessagePartial {
    case LobbyMessage.CreateRoomRequest(userId, roomName) =>
      // This seems to be redundant, but it's give lobby control to decide if user is allowed to create room
      commHub ! CommunicationHub.CreateRoomGame(userId, roomName, GameLogic.adapter(commHub, userId))
      Behaviors.same

    case LobbyMessage.UserJoined(userId) =>
      println(s"User $userId joined. sending ative room List")
      commHub ! LobbyInfo(userId, state.rooms)
      Behaviors.same

    case LobbyMessage.GameRoomCreated(id, owner, ref) =>
      println(s"Room $id created by $owner")
      active(commHub, LobbyState(GameRoom(id, owner, ref) :: state.rooms))
  }
