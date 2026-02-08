package io.scrabit.example.tictactoe.view.game

import tyrian.Html
import tyrian.Html.*
import io.scrabit.example.tictactoe.state.State
import io.scrabit.example.tictactoe.state.game.*
import io.scrabit.example.tictactoe.msg.Msg
import io.scrabit.example.tictactoe.msg.game.GameMsg

object GameView:

  def apply(state: State, game: Game): Html[Msg] =
    if game.isGameOver then gameOverView(state, game)
    else if game.started then boardView(state, game)
    else waitingRoomView(state, game)

  private def waitingRoomView(state: State, game: Game): Html[Msg] =
    div(cls := "game-container")(
      div(cls := "game-header")(
        h2(game.roomName),
        p(s"Room #${game.roomId}")
      ),
      playerStatus(state.username, game.players),
      div(cls := "game-controls")(
        div(cls := "control-buttons")(
          button(
            cls := "nes-btn is-success",
            onClick(Msg.Game(GameMsg.ToggleReady))
          )(readyButtonLabel(state.username, game.players)),
          button(
            cls := "nes-btn is-error",
            onClick(Msg.Game(GameMsg.BackToLobby))
          )("Leave Room")
        )
      )
    )

  private def readyButtonLabel(username: String, players: List[Player]): String =
    val myReady = players.find(_.userId == username).exists(_.isReady)
    if myReady then "Unready" else "Ready"

  private def playerStatus(currentUser: String, players: List[Player]): Html[Msg] =
    div(cls := "player-status nes-container with-title")(
      h3(cls := "title")("Players"),
      if players.isEmpty then p("Waiting for players...")
      else
        div()(
          players.map { player =>
            val isCurrent = player.userId == currentUser
            val playerCls = if isCurrent then "player current-user" else "player"
            val readyCls  = if player.isReady then "ready" else "not-ready"
            div(cls := playerCls)(
              span()(player.userId + (if isCurrent then " (You)" else "")),
              span(cls := readyCls)(if player.isReady then "READY" else "NOT READY")
            )
          }
        )
    )

  private def markBadge(game: Game): Html[Msg] =
    game.myMark match
      case Some(Mark.X) => span(cls := "mark-badge mark-x")("You are: X")
      case Some(Mark.O) => span(cls := "mark-badge mark-o")("You are: O")
      case None         => span()()

  private def boardView(state: State, game: Game): Html[Msg] =
    val turnCls = if game.isMyTurn then "turn-indicator your-turn" else "turn-indicator opponent-turn"

    div(cls := "game-container")(
      div(cls := "game-header")(
        h2(game.roomName),
        markBadge(game),
        p(cls := turnCls)(
          if game.isMyTurn then "Your turn!" else "Opponent's turn..."
        )
      ),
      div(cls := "game-board")(
        renderBoard(game.board, game.isMyTurn)
      ),
      playerStatus(state.username, game.players),
      div(cls := "game-controls")(
        div(cls := "control-buttons")(
          button(
            cls := "nes-btn is-error",
            onClick(Msg.Game(GameMsg.BackToLobby))
          )("Leave Game")
        )
      )
    )

  private def renderBoard(board: Board, isMyTurn: Boolean): Html[Msg] =
    div(cls := "board")(
      board.cells.zipWithIndex.map { case (row, y) =>
        div(cls := "board-row")(
          row.zipWithIndex.map { case (cell, x) =>
            renderCell(cell, x, y, isMyTurn)
          }.toList
        )
      }.toList
    )

  private def renderCell(cell: Cell, x: Int, y: Int, isMyTurn: Boolean): Html[Msg] =
    val isOccupied = cell.mark.isDefined
    val markCls = cell.mark match
      case Some(Mark.X) => " mark-x"
      case Some(Mark.O) => " mark-o"
      case None         => ""
    val baseCls = if isOccupied then "cell occupied" else if isMyTurn then "cell is-primary" else "cell"
    button(
      cls := baseCls + markCls,
      onClick(Msg.Game(GameMsg.MakeMove(x, y))),
      disabled(isOccupied || !isMyTurn)
    )(cell.mark.map(_.toString).getOrElse(""))

  private def gameOverView(state: State, game: Game): Html[Msg] =
    val resultMessage = game.winner match
      case Some(winner) if winner == state.username => ("win-message", "You Win!")
      case Some(winner)                             => ("lose-message", s"$winner Wins!")
      case None                                     => ("draw-message", "It's a Draw!")

    div(cls := "game-over-container")(
      div(cls := "game-over-screen nes-container")(
        h2("Game Over"),
        div(cls := "winner-announcement")(
          div(cls := resultMessage._1)(
            h2(resultMessage._2)
          )
        ),
        div(cls := "final-board")(
          h4("Final Board"),
          renderBoard(game.board, isMyTurn = false)
        ),
        div(cls := "game-over-controls")(
          button(
            cls := "nes-btn is-primary",
            onClick(Msg.Game(GameMsg.BackToLobby))
          )("Back to Lobby")
        )
      )
    )
