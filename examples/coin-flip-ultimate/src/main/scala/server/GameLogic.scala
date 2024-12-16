package server
import io.circe.Json
import io.circe.syntax.*
import io.scrabit.actor.message.RoomMessage.{Action, RoomCreated}
import io.scrabit.actor.message.{OutgoingMessage, RoomMessage}
import org.apache.pekko.actor.typed.receptionist.Receptionist
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

import scala.util.Random

object GameLogic {
    case class CoinHead(uid: String) extends RoomMessage
    case class CoinTail(uid: String) extends RoomMessage

    case class GameResult(userId: String, message: String) extends OutgoingMessage {
      override val tpe: Int = 12

      override def data: Json = Json.obj("message" -> message.asJson)
    }
    
    val initialization: Behavior[RoomMessage] = Behaviors.receiveMessage{
      case RoomCreated(id, owner, commHub) =>
        active(commHub)
    }

    def active(hub: ActorRef[OutgoingMessage]): Behavior[RoomMessage] = Behaviors.setup(context =>
      Behaviors.receiveMessagePartial {
        case Action(uid, actionType, payload) =>
          if (actionType == 10) {
            context.self ! CoinHead(uid)
          } else if (actionType == 11) {
            context.self ! CoinTail(uid)
          }
          Behaviors.same

        case CoinHead(uid) =>
          val isHead  = Random.nextBoolean()
          val message = if isHead then "You bet Head and you won the game" else "You bet Head and you lose the game"
          hub ! GameResult(uid, message)
          Behaviors.same

        case CoinTail(uid) =>
          val isHead  = Random.nextBoolean()
          val message = if isHead then "You bet Tail and you lose the game" else "You bet Tail and you won the game"
          hub ! GameResult(uid, message)
          Behaviors.same
      }
    )
}