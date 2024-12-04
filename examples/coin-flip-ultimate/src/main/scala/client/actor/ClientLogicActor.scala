package client.actor

import client.message.IncomingMessage.LoginSuccess
import client.message.Request.Login
import client.message.{IncomingMessage, Request}
import org.apache.pekko.actor.typed.{ActorRef, Behavior, PostStop}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.http.scaladsl.model.ws.Message as WSMessage

object ClientLogicActor {
  case class Account(username: String, password: String)
  
  sealed trait Message
  type Msg = Message | IncomingMessage

  case object Connected extends Message

  def apply(webSocketRef: ActorRef[Request], account: Account): Behavior[Msg] = Behaviors.receiveMessagePartial {
    case Connected =>
      println("Connected to server. trying to login...")
      webSocketRef ! Request.Login(account.username, account.password)
      Behaviors.same

    case LoginSuccess(userId, sessionKey) =>
      println(s"Login success with ${userId} sk: $sessionKey")
      Behaviors.same
  }

}
