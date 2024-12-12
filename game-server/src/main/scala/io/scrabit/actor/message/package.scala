package io.scrabit.actor

import org.apache.pekko.actor.typed.ActorRef
import io.circe.JsonObject

package object message {

  trait RoomMessage // Request from user.
// NOTE: Rename to "system message" (?)
  object RoomMessage {

    import io.github.iltotore.iron.*
    import io.github.iltotore.iron.constraint.numeric.*

    type ActionType = Int :| Greater[9]

    //Kind of "init message" giving the context when the room is created, e.g: owner, roomId
    case class RoomCreated(owner: String) extends RoomMessage

    case class UserJoined(userId: String) extends RoomMessage

    case class Action(userId: String, tpe: ActionType, payload: Option[JsonObject]) extends RoomMessage
  }

  // TODO: admin request: kill room / kick user, ...

}
