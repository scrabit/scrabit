package io.scrabit.actor

import org.apache.pekko.actor.typed.ActorRef
import io.circe.JsonObject

package object message {

  trait RoomMessage // Request from user.

  object RoomMessage {

    import io.github.iltotore.iron.*
    import io.github.iltotore.iron.constraint.numeric.*

    type ActionType = Int :| Greater[9]

    case class RoomCreated(owner: String, id: Int, ref: ActorRef[RoomMessage]) extends RoomMessage

    case class UserJoined(userId: String) extends RoomMessage

    trait Action extends RoomMessage {
      def userId: String
      def tpe: ActionType
      def payload: Option[JsonObject]
    }
  }

  // TODO: admin request: kill room / kick user, ...

}
