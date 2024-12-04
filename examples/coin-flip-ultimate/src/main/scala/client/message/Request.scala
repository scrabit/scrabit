package client.message

import org.apache.pekko.http.scaladsl.model.ws.Message
import org.apache.pekko.http.scaladsl.model.ws.TextMessage

sealed trait Request {
  def toWsMessage: Message
}

object Request {

  case class Login(username: String, password: String) extends Request {
    override def toWsMessage: Message =
      TextMessage.Strict(s"LOGIN-$username/$password")
  }

}
