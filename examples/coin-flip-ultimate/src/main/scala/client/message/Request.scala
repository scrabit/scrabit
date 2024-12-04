package client.message

import org.apache.pekko.http.scaladsl.model.ws.Message
import org.apache.pekko.http.scaladsl.model.ws.TextMessage
import io.scrabit.actor.message.IncomingMessage.Request.*
import io.circe.JsonObject
import io.circe.Json
import io.circe.syntax.*

sealed trait Request {
  def toWsMessage: Message
}

object Request {
  
  private val COIN_HEAD = 10
  private val COIN_TAIL = 11

  case class Login(username: String, password: String) extends Request {
    override def toWsMessage: Message =
      TextMessage.Strict(s"LOGIN-$username/$password")
  }

  trait UserRequest extends Request {
    def sessionKey: String
    def tpe: Int
    def payload: Option[Json]

    override def toWsMessage: Message = TextMessage.Strict(
      Json
        .obj(
          "sessionKey" -> sessionKey.asJson,
          "tpe"        -> tpe.asJson,
          "payload"    -> payload.asJson
        )
        .noSpaces
    )
  }

  case class CreateRoom(sessionKey: String, name: String) extends UserRequest {

    override def tpe: Int = CREATE_ROOM

    override def payload: Option[Json] = Some(Json.obj("roomName" -> name.asJson))
  }
  
  case class CoinHead(sessionKey: String) extends UserRequest {
    override def tpe: Int = COIN_HEAD
    override def payload: Option[Json] = None
  }

  case class CoinTail(sessionKey: String) extends UserRequest {
    override def tpe: Int = COIN_TAIL
    override def payload: Option[Json] = None
  }
}
