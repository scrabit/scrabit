package client.actor

import client.message.IncomingMessage.{GameResult, LoginSuccess, RoomJoined}
import client.message.Request.{CoinHead, Login}
import client.message.{IncomingMessage, Request}
import org.apache.pekko.actor.typed.{ActorRef, Behavior, PostStop}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.http.scaladsl.model.ws.Message as WSMessage

import scala.annotation.tailrec

object ClientLogicActor {
  case class Account(username: String, password: String)

  sealed trait Message
  type Msg = Message | IncomingMessage

  case object Connected extends Message

  def apply(websocketRef: ActorRef[Request], account: Account): Behavior[Msg] = Behaviors.receiveMessagePartial {
    case Connected =>
      println("Connected to server. trying to login...")
      websocketRef ! Request.Login(account.username, account.password)
      Behaviors.same

    case LoginSuccess(userId, sessionKey) =>
      println(s"User $userId has logged in successfully")
      websocketRef ! Request.CreateRoom(sessionKey, "happy")
      new Actor(websocketRef, sessionKey).joiningRoom()
  }

  private class Actor(websocketRef: ActorRef[Request], sessionKey: String) {

    @tailrec
    private def getUserChoice: String = {
      println("Please bet: head or tail ?")
      val choice = scala.io.StdIn.readLine().trim.toLowerCase
      choice match
        case "h" | "t" | "head" | "tail" => choice
        case _ =>
          println("Invalid choice. Please enter either 'head' (h) or 'tail' (t).")
          getUserChoice
    }


    def joiningRoom(): Behavior[Msg] = Behaviors.receiveMessagePartial { 
      case RoomJoined(userId, roomId) =>
        println(s"user $userId has joined room $roomId")
        val choice = getUserChoice
        if choice == "h" || choice == "head" then {
          websocketRef ! Request.CoinHead(sessionKey)
        } else
          websocketRef ! Request.CoinTail(sessionKey)
        Behaviors.same
        
      case GameResult(msg) =>
        println(msg)
        Behaviors.same
    }

  }

}
