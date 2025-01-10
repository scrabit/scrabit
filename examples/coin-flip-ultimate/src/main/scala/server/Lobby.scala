package server

import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import io.scrabit.actor.message.LobbyMessage
import org.apache.pekko.actor.typed.ActorRef
import io.scrabit.actor.CommunicationHub
import io.scrabit.actor.message.OutgoingMessage

object Lobby:

  val initializing: Behavior[LobbyMessage] = Behaviors.receiveMessagePartial { case LobbyMessage.Init(commHub) =>
    println("Lobby initialized")
    active(commHub)
  }

  private def active(commHub: ActorRef[CommunicationHub.Message]): Behavior[LobbyMessage] = Behaviors.receiveMessagePartial {
    case LobbyMessage.CreateRoomRequest(userId, roomName) =>
      commHub ! CommunicationHub.CreateRoomGame(userId, roomName, GameLogic(commHub))
      Behaviors.same

    case LobbyMessage.GameRoomCreated(id, owner, ref) =>
      Behaviors.same
  }
