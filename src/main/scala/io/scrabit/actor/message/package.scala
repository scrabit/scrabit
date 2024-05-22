package io.scrabit.actor

import org.apache.pekko.actor.typed.ActorRef
import io.circe.JsonObject

package object message {

  trait RoomMessage // Request from user.


  object RoomMessage {
    case class RoomCreated(owner: String, id: Int, ref: ActorRef[RoomMessage]) extends RoomMessage

    case class UserJoined(userId: String) extends RoomMessage

    case class Request(userId: String, tpe: Int, payload: Option[JsonObject]) extends RoomMessage
  }

  //TODO: admin request: kill room / kick user, ...
  
}

