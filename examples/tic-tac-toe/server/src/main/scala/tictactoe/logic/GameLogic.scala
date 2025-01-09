package tictactoe.logic

import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import GameLogic.Msg.*
import io.scrabit.actor.message.OutgoingMessage
import org.apache.pekko.actor.typed.ActorRef
import io.scrabit.actor.message.RoomMessage
import io.scrabit.actor.message.IncomingMessage.Request

object GameLogic:

  enum GameState:
    case Waiting(player1: Player, player2: Option[Player] = None)
    case Playing(player1: Player, player2: Player)
    case Finished

  case class Player(username: String, isReady: Boolean = false)

  enum Msg:
    case Join(player: Player)
    case Ready(username: String)
    case Move(player: Player, x: Int, y: Int)

  private def apply(hub: ActorRef[OutgoingMessage], username: String): Behavior[Msg] = Behaviors.setup { context =>
    val player1 = Player(username)
    waiting(GameState.Waiting(player1, None))
  }

  private def waiting(state: GameState): Behavior[Msg] = Behaviors.receiveMessage { case Ready(username) =>
    Behaviors.same
  }

  private def playing(state: GameState.Playing): Behavior[Msg] = Behaviors.receiveMessage { case Move(player, x, y) =>
    Behaviors.same
  }

  def adapter(hub: ActorRef[OutgoingMessage], username: String): Behavior[RoomMessage] = Behaviors.setup(context =>
    val gameLogic = context.spawnAnonymous(GameLogic(hub, username))
    val convert: RoomMessage => Msg = {
      // case Action(uid, actionType, payload) =>
      //   if (actionType == 10) {
      //     context.self ! CoinHead(uid)
      //   } else if (actionType == 11) {
      //     context.self ! CoinTail(uid)
      //   }
      //   Behaviors.same

      case Request.JoinRoom() => Msg.Join(Player("ahahahahh"))

    }
    Behaviors.receiveMessage[RoomMessage] { msg =>
      gameLogic ! convert(msg)
      Behaviors.same
    }
  )
