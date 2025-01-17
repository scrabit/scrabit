package example

import cats.effect.IO
import tyrian.Html.*
import tyrian.*
import tyrian.cmds.Logger
import tyrian.websocket.*
import scala.scalajs.js.annotation.*
import io.circe.Json
import io.circe.Decoder
import io.circe.generic.semiauto._
import io.circe.parser.*
import io.circe.syntax.*
import example.ServerMessage.*
import io.circe.Encoder
import io.circe.JsonObject

@JSExportTopLevel("TyrianApp")
object Main extends TyrianIOApp[Msg, Model]:

  def router: Location => Msg = Routing.none(Msg.NoOp)

  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    (Model.init, Cmd.None)

  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) =
    case Msg.WebSocketStatus(status) =>
      val (nextWS, cmds) = model.echoSocket.update(status)
      (model.copy(echoSocket = nextWS), cmds)

    case Msg.FromSocket(message) =>
      val logWS = Logger.info[IO]("Got: " + message)
      val msg = ServerMessage.parse(message) match
        case None =>
          Msg.NoOp
        case Some(LoginSuccess(sessionKey)) =>
          Msg.UpdateSessionKey(sessionKey)

        case Some(LoginFailed(msg)) =>
          Msg.NoOp

        case Some(LobbyInfo(rooms)) =>
          Msg.UpdateLobby(rooms)

        case Some(JoinedRoom(id)) =>
          Msg.JoinRoom(id, "changeme")

      val cmds = Cmd.Batch(logWS, Cmd.Emit(msg))
      (model.copy(log = message :: model.log), cmds)

    case Msg.Login(username, password) =>
      val cmds: Cmd[IO, Msg] =
        Cmd.Batch(
          Logger.info(s"login $username/$password"),
          model.echoSocket.publish(s"LOGIN-$username/$password")
        )

      (model, cmds)

    case Msg.UpdateLobby(rooms) =>
      (model.updateRoomList(rooms), Cmd.None)

    case Msg.JoinRoom(id, name) =>
      (model.joinedRoom(id, name), Cmd.None)

    case Msg.SendRequest(request) =>
      model.lobby.map(_.sessionKey) match
        case None =>
          (model, Logger.error("no session key"))
        case Some(key) =>
          val requestWithSessionKey = (request.asJsonObject.add("sessionKey", key.asJson))
          (model, model.echoSocket.publish(requestWithSessionKey.asJson.noSpaces))

    case Msg.UpdateSessionKey(key) =>
      (model.updateSessionKey(key), Cmd.None)

    case Msg.NoOp =>
      (model, Cmd.None)

  def view(model: Model): Html[Msg] =
    div(
      div(
        model.echoSocket.connectDisconnectButton,
        p(button(cls := "button text-green-800", onClick(Msg.Login("scrabit", "tops3cret")))("Login")),
        p("Log:"),
        p(button(cls := "button text-green-800", onClick(Msg.SendRequest(CreateRoomRequest("scrabit"))))("Create room")),
        p(model.log.flatMap(msg => List(text(msg), br)))
      )
    )

  def subscriptions(model: Model): Sub[IO, Msg] =
    model.echoSocket.subscribe {
      case WebSocketEvent.Error(errorMesage) =>
        Msg.FromSocket(errorMesage)

      case WebSocketEvent.Receive(message) =>
        Msg.FromSocket(message)

      case WebSocketEvent.Open =>
        Msg.FromSocket("Connected to game server")

      case WebSocketEvent.Close(code, reason) =>
        Msg.FromSocket(s"<socket closed> - code: $code, reason: $reason")

      case WebSocketEvent.Heartbeat =>
        Msg.NoOp
    }

class Request(val tpe: Int, val payload: Json)

object Request {
  given Encoder.AsObject[Request] = Encoder.AsObject.instance(req => JsonObject("tpe" -> req.tpe.asJson, "payload" -> req.payload))
}

class CreateRoomRequest(roomName: String) extends Request(3, Json.obj("roomName" -> roomName.asJson))

enum Msg:
  case FromSocket(message: String)
  case ToSocket(message: String)
  case Login(username: String, password: String)
  case UpdateSessionKey(sessionKey: String)
  case SendRequest(request: Request)
  case UpdateLobby(rooms: List[Room])
  case JoinRoom(id: Int, name: String)
  case WebSocketStatus(status: Socket.Status)
  case NoOp

final case class Model(echoSocket: Socket, log: List[String], lobby: Option[Lobby], game: Option[Game]) {
  def loggedIn: Boolean = lobby.nonEmpty
  def updateRoomList(rooms: List[Room]): Model =
    lobby match
      case None =>
        copy(lobby = Some(Lobby(rooms, "")))
      case Some(lb) =>
        copy(lobby = Some(lb.copy(rooms = rooms)))

  def updateSessionKey(key: String): Model =
    lobby match
      case None =>
        copy(lobby = Some(Lobby(Nil, key)))
      case Some(lb) =>
        copy(lobby = Some(lb.copy(sessionKey = key)))

  def joinedRoom(roomId: Int, roomName: String): Model =
    copy(game = Some(Game(roomName = roomName, roomId = roomId, players = Nil, isMyTurn = false, Board.empty)))

}

final case class Room(id: Int, owner: String, name: String)
final case class Lobby(rooms: List[Room], sessionKey: String)

final case class Game(roomName: String, roomId: Int, players: List[Player], isMyTurn: Boolean, board: Board)

final case class Player(userId: String, isReady: Boolean)

enum Mark:
  case X
  case O

case class Cell(mark: Option[Mark])

case class Board(cells: Vector[Vector[Cell]])

object Board {
  val empty: Board            = Board(13)
  def apply(size: Int): Board = Board(Vector.fill(size, size)(Cell(None)))
}

object Model:
  val init: Model =
    Model(Socket.init, Nil, None, None)

/**
 * Encapsulates and manages our socket connection, cleanly proxies methods, and knows how to draw
 * the right connnect/disconnect button.
 */
final case class Socket(socketUrl: String, socket: Option[WebSocket[IO]]):

  def connectDisconnectButton =
    if socket.isDefined then button(onClick(Socket.Status.Disconnecting.asMsg))("Disconnect")
    else button(onClick(Socket.Status.Connecting.asMsg))("Connect")

  def update(status: Socket.Status): (Socket, Cmd[IO, Msg]) =
    status match
      case Socket.Status.ConnectionError(err) =>
        (this, Logger.error(s"Failed to open WebSocket connection: $err"))

      case Socket.Status.Connected(ws) =>
        (this.copy(socket = Some(ws)), Cmd.None)

      case Socket.Status.Connecting =>
        val connect =
          WebSocket.connect[IO, Msg](
            address = socketUrl,
            keepAliveSettings = KeepAliveSettings.default
          ) {
            case WebSocketConnect.Error(err) =>
              Socket.Status.ConnectionError(err).asMsg

            case WebSocketConnect.Socket(ws) =>
              Socket.Status.Connected(ws).asMsg
          }

        (this, connect)

      case Socket.Status.Disconnecting =>
        val log = Logger.info[IO]("Graceful shutdown of EchoSocket connection")
        val cmds =
          socket.map(ws => Cmd.Batch(log, ws.disconnect)).getOrElse(log)

        (this.copy(socket = None), cmds)

      case Socket.Status.Disconnected =>
        (this, Logger.info("WebSocket not connected yet"))

  def publish(message: String): Cmd[IO, Msg] =
    socket.map(_.publish(message)).getOrElse(Cmd.None)

  def subscribe(toMessage: WebSocketEvent => Msg): Sub[IO, Msg] =
    socket.fold(Sub.emit[IO, Msg](Socket.Status.Disconnected.asMsg)) {
      _.subscribe(toMessage)
    }

enum ServerMessage:
  case LoginSuccess(sessionKey: String)
  case LoginFailed(msg: String)
  case LobbyInfo(rooms: List[Room]) // Should NOT use class Room
  case JoinedRoom(id: Int)

object ServerMessage {
  val LOGIN_SUCCESS = 0
  val LOGIN_FAILED  = 1
  val JOINED_ROOM   = 2
  val LOBBY_INFO    = 10

  given Decoder[LoginSuccess] = deriveDecoder[LoginSuccess]
  given Decoder[LobbyInfo]    = deriveDecoder[LobbyInfo]
  given Decoder[Room]         = deriveDecoder[Room]
  given Decoder[JoinedRoom]   = deriveDecoder[JoinedRoom]

  case class TypedMessage(tpe: Int, payload: Json)
  given Decoder[TypedMessage] = deriveDecoder[TypedMessage]

  def parse(text: String): Option[ServerMessage] =

    def decodeAsOption[T <: ServerMessage: Decoder](msg: TypedMessage): Option[T] =
      msg.payload.as[T] match
        case Left(e) =>
          None
        case Right(v) =>
          Some(v)

    decode[TypedMessage](text).toOption.flatMap { typedMsg =>
      typedMsg.tpe match
        case LOGIN_SUCCESS =>
          decodeAsOption[LoginSuccess](typedMsg)
        case LOBBY_INFO =>
          decodeAsOption[LobbyInfo](typedMsg)
        case JOINED_ROOM =>
          decodeAsOption[JoinedRoom](typedMsg)
        case _ => None
    }
}
object Socket:

  val init: Socket =
    Socket("ws://localhost:8080/", None)

  enum Status:
    case Connecting
    case Connected(ws: WebSocket[IO])
    case ConnectionError(msg: String)
    case Disconnecting
    case Disconnected

    def asMsg: Msg = Msg.WebSocketStatus(this)
