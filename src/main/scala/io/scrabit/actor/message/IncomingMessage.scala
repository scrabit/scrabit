package io.scrabit.actor.message

import io.circe._, io.circe.syntax._, io.circe.generic.semiauto._, io.circe.parser._
import org.apache.pekko.http.scaladsl.model.ws.Message
import org.apache.pekko.actor.typed.ActorRef


sealed trait IncomingMessage

object IncomingMessage:
  case class RawMessage(text: String, connection: ActorRef[OutgoingMessage]) extends IncomingMessage // RawMessage -> Request / Login / discarded
  case class Request(val userId: String, // userId is reliable because session is already verified
                     val tpe: Int,
                     val payload: Option[JsonObject]) extends IncomingMessage {
    def toRoomMessage: RoomMessage = RoomMessage.Request(userId, tpe, payload)
  }
  
  object Request {
    private val JOIN_ROOM = 3
    private val CREATE_ROOM = 4
    private val READY = 5
    private val PLAYER_REPLY = 8
  
    object JoinRoom {
      def unapply(req: Request): Option[(String, String)] = {
        if (req.tpe == JOIN_ROOM) {
          req.payload.flatMap(json =>
              json("roomId").flatMap(_.asString).map(roomId =>
                (req.userId, roomId)
              )
          )
        } else None
      }
    }
  
    object CreateRoom {
      def unapply(req: Request): Option[(String, String)] = {
        if (req.tpe == CREATE_ROOM) {
          req.payload.flatMap(json =>
              json("roomName").flatMap(_.asString).map(roomName =>
                (req.userId, roomName)
              )
          )
        } else None
      }
    }
  }
  
  
  object SessionMessage {
    def unapply(m: RawMessage): Option[(String, Int, Option[JsonObject])] = for {
      json <- parse(m.text).toOption
      obj <- json.asObject
      sessionKey <- obj("sessionKey").flatMap(_.asString)
      tpe <- obj("tpe").flatMap(_.asNumber).flatMap(_.toInt)
      data = obj("payload").flatMap(_.asObject)
    } yield (sessionKey, tpe, data)
  }
  
  object Login {
    private val PREFIX = "LOGIN-" // LOGIN-<userId>/<password>
    private val userIdRegex = s"$PREFIX(.+)/(.+)".r
  
      def parseCredentials(text: String): Option[(String, String)] = {
        userIdRegex.findFirstMatchIn(text).map(matched =>
          (matched.group(1), matched.group(2))
        )
    }
  
    def unapply(m: RawMessage): Option[(String, String, ActorRef[OutgoingMessage])] =
      parseCredentials(m.text).map((userId, password) => (userId, password, m.connection))
  }
