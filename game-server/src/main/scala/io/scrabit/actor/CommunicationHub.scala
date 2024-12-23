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

  case class UserSession(userId: String, sessionKey: String, connection: ActorRef[OutgoingMessage], roomId: Option[Int]) {

    def joinRoom(roomId: Int): UserSession = this.copy(roomId = Some(roomId))
  }

  case class CreateSystemRoom(name: String) extends InternalMessage

  case class UserDisconnected(userId: String, connection: ActorRef[OutgoingMessage]) extends InternalMessage

  case class SessionCreated(userId: String, sessionKey: String, connection: ActorRef[OutgoingMessage]) extends InternalMessage

  case class JoinRoom(userId: String, roomId: Int) extends InternalMessage

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

      this.copy(users = users + (userId -> UserSession(userId, sessionKey, connection, None)), sessions = updatedSessions)
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

  def create(authenticator: ActorRef[AuthenticationService.Login],
             roomBehavior: Behavior[RoomMessage], 
            ): Behavior[Message] = Behaviors.setup { context =>
    context.system.receptionist ! Receptionist.Register(CommunicationHubServiceKey, context.self)
    Actor(authenticator, roomBehavior, context)(Data.empty)
  }

  private class Actor(
    authenticationService: ActorRef[AuthenticationService.Login],
    roomBehavior: Behavior[RoomMessage],
    context: ActorContext[Message]
  ) {
    def apply(data: Data): Behavior[Message] = Behaviors.receiveMessagePartial {
      case IncomingMessage.Login(userId, password, connection) =>
        if (data.users.contains(userId)) {
          context.log.warn(s"User $userId already Logged in")
        } else {
          authenticationService ! AuthenticationService.Login(userId, password, connection, context.self)
        }
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

      case CreateRoom(owner, roomName) =>
        val spawnActor: Int => ActorRef[RoomMessage] = roomId => context.spawn(roomBehavior, s"room-${roomId}")
        data.addRoom(spawnActor) match {
          case None =>
            context.log.warn("Cannot create new room: reached max room limit")
            Behaviors.same
          case Some((updatedData, roomId, ref)) =>
            context.self ! JoinRoom(owner, roomId)
            ref ! RoomCreated(roomId, owner, context.self)
            context.log.info(s"Created room $roomName - id: $roomId")
            apply(updatedData)
        }

      case CreateSystemRoom(roomName) =>
        val spawnActor: Int => ActorRef[RoomMessage] = roomId => context.spawn(roomBehavior, s"room-${roomId}")
        data.addRoom(spawnActor) match {
          case None =>
            context.log.warn("Cannot create new room: reached max room limit")
            Behaviors.same
          case Some((updatedData, roomId, ref)) =>
            context.log.info(s"Created room $roomName - id: $roomId")
            apply(updatedData)
        }

      case JoinRoom(userId, roomId) =>
        // TODO: sync room state (new user) with Room Actor (?)
        data.userJoinRoom(userId, roomId) match {
          case Left(msg) =>
            context.log.warn(s"Failed to join. $msg")
            Behaviors.same
          case Right(updatedData) =>
            context.self ! UserJoinedRoom(userId, roomId)
            apply(updatedData)
        }

      case req: Request =>
        data.users.get(req.userId) match {
          case None =>
            context.log.error(s"userId ${req.userId} not exist")
          case Some(session) =>
            session.roomId foreach { roomId =>
              req.toAction foreach { action =>
                data.rooms(roomId) ! action
              }
            }
        }
        Behaviors.same

      case out: OutgoingMessage =>
        val recipient = out.userId
        context.log.debug(s"out <-- $recipient: ${out.toWsMessage}")
        data.users.get(recipient) match
          case None =>
            context.log.error(s"Error: $recipient not found")
          case Some(userSession) =>
            userSession.connection ! out
        Behaviors.same
    }
  }

  //   case SessionMessage(sessionId, roomId, tpe, jsonData) =>
  //     data.sessions.get(sessionId) match {
  //       case None =>
  //         context.log.warn(s"Received not authenticated message with session Id : $sessionId")
  //       case Some(userId) =>
  //         context.log.debug(s"In --> $userId type-$tpe roomId-$roomId data: $jsonData")
  //         context.self ! UserRequest(userId, tpe, roomId, jsonData)
  //     }
  //     Behaviors.same
  //
  //
  //   case UserRequest.CreateRoom(userId, roomName) =>
  //       val roomId = s"${101 + data.rooms.size}"
  //       val owner = Player.create(userId)
  //       val room = context.spawn(Room.create(roomId, roomName, owner, context.self, lobby), s"room-$roomId")
  //       val updatedData = data.addRoom(roomId, room)
  //       context.log.info(s"Created room with id: $roomId.")
  //       lobby ! RoomCreated(roomId, roomName)
  //       live(updatedData)
  //
  //   case UserRequest.JoinRoom(userId, roomId) =>
  //     data.rooms.get(roomId) foreach { roomRef =>
  //       context.log.debug(s"user $userId joining room $roomId")
  //       roomRef ! Room.JoinRoom(userId)
  //     }
  //     Behaviors.same
  //
  //   case UserRequest.Ready(userId, roomId) =>
  //     data.rooms.get(roomId) foreach { roomRef =>
  //       roomRef ! Room.ReadyToggle(userId)
  //     }
  //     Behaviors.same
  //
  //   case UserRequest.PlayerReply(userId, roomId, answer) =>
  //     data.rooms.get(roomId) foreach { roomRef =>
  //       roomRef ! Room.PlayerReply(userId, answer)
  //     }
  //     Behaviors.same
  //
  //
  //
  //
  //   case m@ _ =>
  //     context.log.warn(s"unhandled message $m")
  //     Behaviors.same

end CommunicationHub
