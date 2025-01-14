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

    type ActionType = Int :| Greater[9]
    given Encoder[ActionType] = Encoder.encodeInt.contramap(identity)

    // Kind of "init message" giving the context when the room is created, e.g: owner, roomId
    case class RoomCreated(id: Int, owner: String, commHub: ActorRef[CommunicationHub.Message]) extends RoomMessage

    case class UserJoined(userId: String) extends RoomMessage

    case class Action(userId: String, tpe: ActionType, payload: Option[JsonObject]) extends RoomMessage
  }

  // TODO: admin request: kill room / kick user, ...

  enum LobbyMessage:
    case Init(commHub: ActorRef[CommunicationHub.Message])
    case UserJoined(userId: String)
    case GameRoomCreated(id: Int, owner: String, ref: ActorRef[RoomMessage])
    case CreateRoomRequest(userId: String, roomName: String)

}
