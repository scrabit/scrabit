package io.scrabit.example.tictactoe.state

import io.scrabit.example.tictactoe.state.connection.ConnectionStatus
import io.scrabit.example.tictactoe.state.auth.Session
import io.scrabit.example.tictactoe.state.auth.LoginFormModel
import io.scrabit.example.tictactoe.state.game.*

case class State(
  connection: ConnectionStatus,
  session: Option[Session],
  loginForm: LoginFormModel,
  lobby: Option[Lobby],
  game: Option[Game]
):
  def isLoggedIn: Boolean = session.isDefined
  def isInGame: Boolean = game.isDefined
  def username: String = loginForm.username

  def withSession(key: String): State =
    copy(
      session = Some(Session(key)),
      lobby = Some(Lobby(Nil, key, ""))
    )

  def withRooms(rooms: List[Room]): State =
    lobby match
      case None     => copy(lobby = Some(Lobby(rooms, session.map(_.sessionKey).getOrElse(""), "")))
      case Some(lb) => copy(lobby = Some(lb.copy(rooms = rooms)))

  def joinedRoom(roomId: Int, roomName: String): State =
    copy(game = Some(Game(roomName, roomId, Nil, isMyTurn = false, Board.empty)))

  def withReadyState(players: List[Player]): State =
    game match
      case None    => this
      case Some(g) => copy(game = Some(g.copy(players = players)))

  def withBoard(currentTurn: String, board: Board): State =
    game match
      case None    => this
      case Some(g) =>
        val myTurn = currentTurn == username
        copy(game = Some(g.copy(board = board, isMyTurn = myTurn, started = true)))

  def withGameResult(winner: Option[String]): State =
    game match
      case None    => this
      case Some(g) => copy(game = Some(g.copy(winner = winner)))

  def leaveGame: State =
    copy(game = None)

object State:
  val init: State = State(
    connection = ConnectionStatus.Disconnected,
    session = None,
    loginForm = LoginFormModel.empty,
    lobby = None,
    game = None
  )
