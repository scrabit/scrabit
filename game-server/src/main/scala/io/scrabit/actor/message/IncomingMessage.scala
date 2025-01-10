package io.scrabit.actor.message

import io.circe.*
import io.circe.parser.*
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.numeric.*
import io.scrabit.actor.message.RoomMessage.Action
import org.apache.pekko.actor.typed.ActorRef

sealed trait IncomingMessage

object IncomingMessage:
  case class RawMessage(text: String, connection: ActorRef[OutgoingMessage]) extends IncomingMessage // RawMessage -> Request / Login / discarded

  case class Request(
    userId: String, // userId is reliable because session is already verified
    tpe: Int,
    payload: Option[JsonObject]
  ) extends IncomingMessage {

    def toAction: Action = Action(userId, tpe, payload)
  }

  object Request {
    private val JOIN_ROOM   = 2
    private val CREATE_ROOM = 3

    object JoinRoom {
      def unapply(req: Request): Option[(String, Int)] =
        if (req.tpe == JOIN_ROOM) {
          req.payload.flatMap(json => json("roomId").flatMap(_.asNumber.flatMap(_.toInt)).map(roomId => (req.userId, roomId)))
        } else None
    }

    object CreateRoom {
      def unapply(req: Request): Option[(String, String)] =
        if (req.tpe == CREATE_ROOM) {
          req.payload.flatMap(json => json("roomName").flatMap(_.asString).map(roomName => (req.userId, roomName)))
        } else None
    }
  }

  object SessionMessage {
    def unapply(m: RawMessage): Option[(String, Int, Option[JsonObject])] = for {
      json       <- parse(m.text).toOption
      obj        <- json.asObject
      sessionKey <- obj("sessionKey").flatMap(_.asString)
      tpe        <- obj("tpe").flatMap(_.asNumber).flatMap(_.toInt)
      data        = obj("payload").flatMap(_.asObject)
    } yield (sessionKey, tpe, data)
  }

  object Login {
    private val PREFIX      = "LOGIN-" // LOGIN-<userId>/<password>
    private val userIdRegex = s"$PREFIX(.+)/(.+)".r

    private def parseCredentials(text: String): Option[(String, String)] =
      userIdRegex.findFirstMatchIn(text).map(matched => (matched.group(1), matched.group(2)))

    def unapply(m: RawMessage): Option[(String, String, ActorRef[OutgoingMessage])] =
      parseCredentials(m.text).map((userId, password) => (userId, password, m.connection))
  }
