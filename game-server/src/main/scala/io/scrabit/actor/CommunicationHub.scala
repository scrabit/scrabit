package io.scrabit.actor

import io.scrabit.actor.CommunicationHub.Data.RoomIdSpace
import io.scrabit.actor.message.*
import io.scrabit.actor.message.IncomingMessage.Request.*
import io.scrabit.actor.message.IncomingMessage.{Request, SessionMessage}
import io.scrabit.actor.message.OutgoingMessage.{LoginSuccess, UserJoinedRoom}
import io.scrabit.actor.message.RoomMessage.RoomCreated
import io.scrabit.actor.session.AuthenticationService
import org.apache.pekko.actor.typed.*
import org.apache.pekko.actor.typed.receptionist.{Receptionist, ServiceKey}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}

object CommunicationHub:

  sealed trait InternalMessage

  type Message = IncomingMessage | OutgoingMessage | InternalMessage

  val CommunicationHubServiceKey = ServiceKey[Message]("communication-hub-service")

  private val LOBBY_ROOM_ID = 0

  case class UserSession(userId: String, sessionKey: String, connection: ActorRef[OutgoingMessage], roomId: Int) {

    def joinRoom(id: Int): UserSession = this.copy(roomId = id)
    def joinLobby: UserSession         = this.copy(roomId = LOBBY_ROOM_ID)
    def isInLobby: Boolean             = roomId == LOBBY_ROOM_ID
  }

  case class CreateRoomGame(owner: String, name: String, Behavior: Behavior[RoomMessage]) extends InternalMessage

  case class UserDisconnected(userId: String, connection: ActorRef[OutgoingMessage]) extends InternalMessage

  case class SessionCreated(userId: String, sessionKey: String, connection: ActorRef[OutgoingMessage]) extends InternalMessage

  case class JoinRoomAccepted(userId: String, roomId: Int) extends InternalMessage

  object Data {

    private val defaultRoomIdSpace = new RoomIdSpace(Array.fill(10000)(true)) // 10000 rooms

    val empty = Data(Map.empty, Map.empty, Map.empty, defaultRoomIdSpace, Set.empty)

    class RoomIdSpace(private val spaces: Array[Boolean]) {
      def takeNextId: Option[(Int, RoomIdSpace)] =
        spaces.zipWithIndex.find((available, _) => available).map { case (_, id) =>
          id -> new RoomIdSpace(spaces = spaces.updated(id, false))
        }
    }
  }

  case class Data(
    users: Map[String, UserSession],
    sessions: Map[String, String], // sessionId -> userid
    rooms: Map[Int, ActorRef[RoomMessage]],
    roomIdSpace: RoomIdSpace,
    connections: Set[ActorRef[OutgoingMessage]]
  ) {

    def newSession(userId: String, sessionKey: String, connection: ActorRef[OutgoingMessage]): Data = {
      val (updatedSessions, updatedConnections) = users.get(userId) match {
        case None =>
          (sessions + (sessionKey -> userId)) -> this.connections
        case Some(userSession) =>
          ((sessions - userSession.sessionKey) + (sessionKey -> userId)) -> (this.connections - userSession.connection)
      }

      this.copy(users = users + (userId -> UserSession(userId, sessionKey, connection, LOBBY_ROOM_ID)), sessions = updatedSessions)
    }

    def deleteSession(userSession: UserSession): Data =
      this.copy(users = users - userSession.userId, sessions = sessions - userSession.sessionKey, connections = connections - userSession.connection)

    def removeConnection(connection: ActorRef[OutgoingMessage]): Data = this.copy(connections = this.connections - connection)

    def addRoom(spawnActor: Int => ActorRef[RoomMessage]): Option[(Data, Int, ActorRef[RoomMessage])] =
      this.roomIdSpace.takeNextId.map { (id, updatedRoomIdSpace) =>
        val actorRef    = spawnActor(id)
        val updatedData = this.copy(roomIdSpace = updatedRoomIdSpace, rooms = rooms + (id -> actorRef))
        (updatedData, id, actorRef)
      }

    def userJoinRoom(userId: String, roomId: Int): Either[String, Data] =
      if (!users.contains(userId)) {
        Left(s"User $userId not found")
      } else if (!rooms.contains(roomId)) {
        Left(s"Room $roomId not found")
      } else {
        Right(this.copy(users = users + (userId -> users(userId).joinRoom(roomId))))
      }

  }

  def create(authenticator: ActorRef[AuthenticationService.Login], lobby: ActorRef[LobbyMessage]): Behavior[Message] = Behaviors.setup { context =>
    lobby ! LobbyMessage.Init(context.self)
    Actor(authenticator, lobby, context)(Data.empty)
  }

  private class Actor(
    authenticationService: ActorRef[AuthenticationService.Login],
    lobby: ActorRef[LobbyMessage],
    context: ActorContext[Message]
  ) {
    def apply(data: Data): Behavior[Message] = Behaviors.receiveMessagePartial {
      case IncomingMessage.Login(userId, password, connection) =>
        // if (data.users.contains(userId)) {
        //   context.log.warn(s"User $userId already Logged in")
        // } else {
        authenticationService ! AuthenticationService.Login(userId, password, connection, context.self)
        Behaviors.same

      case SessionCreated(userId, sessionKey, connection) =>
        //         - Loggin User login from another connection -> override connection
        //         // TODO : test disconnect. Write test case
        if (data.connections.contains(connection)) {
          // cases : - Same connection, new user login (need unwatch)
          context.unwatch(connection)
        }

        context.log.info(s"store new session for $userId with sessionKey $sessionKey")
        val updatedData = data.newSession(userId, sessionKey, connection)
        context.self ! LoginSuccess(userId, sessionKey)
        lobby ! LobbyMessage.LoggedIn(userId)
        context.watchWith(connection, UserDisconnected(userId, connection))
        apply(updatedData)

      case UserDisconnected(userId, connection) =>
        context.log.debug(s"Connection disconnected: $connection from userId: $userId")
        data.users.get(userId) match {
          case None =>
            apply(data.removeConnection(connection))
          case Some(userSession) =>
            if (connection == userSession.connection) {
              apply(data.deleteSession(userSession)) // TODO:need unwatch connection ?
            } else
              apply(data.removeConnection(connection))
        }

      case SessionMessage(sessionKey, tpe, payload) =>
        data.sessions.get(sessionKey) match {
          case None =>
            context.log.warn(s"invalid sessionKey: $sessionKey. Message type $tpe")
          case Some(userId) =>
            context.self ! IncomingMessage.Request(userId, tpe, payload)
        }
        Behaviors.same

      case CreateRoomGame(owner, roomName, behavior) =>
        val spawnActor: Int => ActorRef[RoomMessage] = roomId => context.spawn(behavior, s"room-${roomId}")
        data.addRoom(spawnActor) match {
          case None =>
            context.log.warn("Cannot create new room: reached max room limit")
            Behaviors.same
          case Some((updatedData, roomId, ref)) =>
            context.log.info(s"Created room $roomName - id: $roomId")
            context.self ! JoinRoomAccepted(owner, roomId)
            lobby ! LobbyMessage.GameRoomCreated(roomId, owner, ref)
            apply(updatedData)
        }

      case JoinRoomAccepted(userId, roomId) =>
        data.userJoinRoom(userId, roomId) match {
          case Left(msg) =>
            context.log.info(s"Failed to join. $msg")
            Behaviors.same
          case Right(updatedData) =>
            context.log.info(s"User $userId joined room $roomId")
            context.self ! UserJoinedRoom(userId, roomId)
            apply(updatedData)
        }

      case JoinRoom(userId, roomId) =>
        lobby ! LobbyMessage.JoinRoomRequest(userId, roomId)
        Behaviors.same

      case CreateRoom(userId, roomName) =>
        if data.users.get(userId).exists(_.roomId == LOBBY_ROOM_ID)
        then lobby ! LobbyMessage.CreateRoomRequest(userId, roomName)
        else context.log.warn(s"User $userId sent invalid CreateRoom Request")
        Behaviors.same

      case req: Request =>
        data.users.get(req.userId) match {
          case None =>
            context.log.error(s"userId ${req.userId} not exist")
          case Some(session) =>
            context.log.error(s"in --> ${req.userId}: ${req.toAction}")
            data.rooms.get(session.roomId) foreach { _ ! req.toAction }
        }
        Behaviors.same

      case out: OutgoingMessage =>
        val recipient = out.recipient
        context.log.debug(s"out <-- $recipient: ${out.toWsMessage}")
        data.users.get(recipient) match
          case None =>
            context.log.error(s"Error: $recipient not found")
          case Some(userSession) =>
            userSession.connection ! out
        Behaviors.same
    }
  }

end CommunicationHub
