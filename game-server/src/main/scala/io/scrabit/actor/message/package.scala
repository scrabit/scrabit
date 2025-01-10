package io.scrabit.actor

import org.apache.pekko.actor.typed.ActorRef
import io.circe.JsonObject
import io.scrabit.actor.CommunicationHub

import java.util.function.Consumer
import io.circe.Encoder

package object message {

  trait RoomMessage // Request from user.

// NOTE: Rename to "system message" (?)
  object RoomMessage {

    import io.github.iltotore.iron.*
    import io.github.iltotore.iron.constraint.numeric.*

    type ActionType = Int
    given Encoder[ActionType] = Encoder.encodeInt.contramap(identity)

    // Kind of "init message" giving the context when the room is created, e.g: owner, roomId
    case class RoomCreated(id: Int, owner: String, commHub: ActorRef[CommunicationHub.Message]) extends RoomMessage

    case class UserJoined(userId: String) extends RoomMessage

    case class Action(userId: String, tpe: ActionType, payload: Option[JsonObject]) extends RoomMessage

    object Action {
      // yield this control of these messagge to Game Room
      private val JOIN_ROOM = 3
      private val READY     = 5

      object JoinRoom {
        def unapply(action: Action): Option[String] =
          if (action.tpe == JOIN_ROOM) {
            Some(action.userId)
          } else None
      }

      object Ready {
        def unapply(action: Action): Option[String] =
          if (action.tpe == READY) {
            Some(action.userId)
          } else None
      }
    }

  }

  enum LobbyMessage:
    case Init(commHub: ActorRef[CommunicationHub.Message])
    case LoggedIn(userId: String)
    case CreateRoomRequest(userId: String, roomName: String)
    case GameRoomCreated(id: Int, owner: String, ref: ActorRef[RoomMessage])
    case JoinRoomRequest(userId: String, roomId: Int)

}
