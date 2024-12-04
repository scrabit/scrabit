package server
import io.circe.Json
import io.circe.syntax.*
import io.scarabit.actor.CommunicationHub.{CommunicationHubServiceKey, SetRoomBehavior}
import io.scrabit.actor.message.{OutgoingMessage, RoomMessage}
import io.scrabit.actor.message.RoomMessage.Action
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.receptionist.Receptionist
import org.apache.pekko.actor.typed.scaladsl.Behaviors

import scala.util.Random

object GameRoom {

  def apply(): Behavior[RoomMessage | Receptionist.Listing] = Behaviors.setup { context =>

    context.system.receptionist ! Receptionist.Subscribe(CommunicationHubServiceKey, context.self)
    Behaviors.receiveMessagePartial { case CommunicationHubServiceKey.Listing(hubs) =>
      if (hubs.nonEmpty) {
        val comHub = hubs.head
        comHub ! SetRoomBehavior(GameLogic(comHub)) // Move this logic to bootstrap actor
      }
      Behaviors.same

    }
  }

  object GameLogic {
    case class CoinHead(uid: String) extends RoomMessage
    case class CoinTail(uid: String) extends RoomMessage

    case class GameResult(userId: String, message: String) extends OutgoingMessage {
      override val tpe: Int = 12

      override def data: Json = Json.obj("message" -> message.asJson)
    }

    def apply(hub: ActorRef[OutgoingMessage]): Behavior[RoomMessage] = Behaviors.setup(context =>
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

}
