package io.scrabit.actor.message

import org.apache.pekko.http.scaladsl.model.ws.Message
import io.circe.Json, io.circe.syntax._
import io.circe.JsonObject
import org.apache.pekko.http.scaladsl.model.ws.TextMessage

trait OutgoingMessage {
  def recipients: List[String]
  def tpe: Int
  def data: Json
  def toWsMessage: Message = TextMessage.Strict(
    Json
      .obj(
        "tpe" -> tpe.asJson,
        "data" -> data
      )
      .noSpaces
  )
}

object OutgoingMessage:

  private val LOGIN_SUCCESS = 0
  private val USER_JOINED_ROOM = 2
  private val USER_READY = 5
  private val GAME_START = 6
  private val NEW_ROUND = 7
  private val ROUND_END = 9

  case class LoginSuccess(userId: String, sessionKey: String) extends OutgoingMessage {
    override val tpe: Int = LOGIN_SUCCESS

    override def recipients: List[String] = List(userId)

    override def data: Json = Json.obj("sessionKey" -> sessionKey.asJson)
  }

  case class UserJoinedRoom(userId: String, roomId: Int) extends OutgoingMessage {
    override val tpe: Int = USER_JOINED_ROOM

    override def recipients: List[String] = List(userId)

    override def data: Json = Json.obj("roomId" -> roomId.asJson)
  }

end OutgoingMessage
