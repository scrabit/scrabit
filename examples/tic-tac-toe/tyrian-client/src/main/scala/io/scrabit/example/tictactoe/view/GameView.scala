package io.scrabit.example.tictactoe.view

import tyrian.Html
import tyrian.Html.*
import io.scrabit.example.tictactoe.model.State.*
import io.scrabit.example.tictactoe.model.{Msg, State}

object GameView {

  def apply(state: State): Html[Msg] = state.game match {
    case None => 
      div(cls := "no-game")(
        h3("No game active"),
        button(cls := "nes-btn", onClick(Msg.BackToLobby))("Back to Lobby")
      )
    case Some(game) => 
      if (game.isGameOver) {
        gameOverView(game, state.loginState.username)
      } else {
        div(cls := "game-container")(
          gameHeader(game, state.loginState.username),
          playerStatus(game.players, state.loginState.username),
          GameBoard(game.board, game.isMyTurn && !game.isGameOver),
          gameControls(game.players, state.loginState.username)
        )
      }
  }

  private def gameHeader(game: Game, currentUser: String): Html[Msg] =
    div(cls := "game-header nes-container with-title")(
      h3(cls := "title")(s"Room: ${game.roomName}"),
      p(s"Room ID: ${game.roomId}")
    )

  private def playerStatus(players: List[Player], currentUser: String): Html[Msg] =
    div(cls := "player-status nes-container")(
      h4("Players:"),
      if (players.isEmpty) {
        p("Waiting for players...")
      } else {
        div(
          players.map { player =>
            div(cls := s"player ${if player.userId == currentUser then "current-user" else ""}")(
              span(s"${player.userId}"),
              if (player.isReady) 
                span(cls := "ready")(" ✓ Ready") 
              else 
                span(cls := "not-ready")(" ⏳ Not Ready")
            )
          }*
        )
      }
    )

  private def gameControls(players: List[Player], currentUser: String): Html[Msg] =
    div(cls := "game-controls")(
      div(cls := "control-buttons")(
        button(cls := "nes-btn", onClick(Msg.ToggleReady))("Toggle Ready"),
        button(cls := "nes-btn", onClick(Msg.BackToLobby))("Back to Lobby")
      )
    )

  private def gameOverView(game: Game, currentUser: String): Html[Msg] =
    div(cls := "game-over-container")(
      div(cls := "game-over-screen nes-container with-title")(
        h3(cls := "title")("🎮 Game Over!"),
        div(cls := "winner-announcement")(
          game.winner match {
            case Some(winner) =>
              if (winner == currentUser) {
                div(cls := "win-message")(
                  h2("🎉 You Won! 🎉"),
                  p(s"Congratulations, $winner!")
                )
              } else {
                div(cls := "lose-message")(
                  h2("😔 You Lost"),
                  p(s"$winner won this round!")
                )
              }
            case None =>
              div(cls := "draw-message")(
                h2("🤝 It's a Draw!"),
                p("Good game, everyone!")
              )
          }
        ),
        div(cls := "final-board")(
          h4("Final Board:"),
          GameBoard(game.board, isMyTurn = false)
        ),
        div(cls := "game-over-controls")(
          button(cls := "nes-btn is-primary", onClick(Msg.RestartGame))("🔄 Play Again"),
          button(cls := "nes-btn", onClick(Msg.BackToLobby))("🏠 Back to Lobby")
        )
      )
    )
}

object GameBoard {

  def apply(board: Board, isMyTurn: Boolean): Html[Msg] =
    div(cls := "game-board")(
      h4(if (isMyTurn) "Your Turn!" else "Opponent's Turn"),
      div(cls := "board nes-container")(
        board.cells.zipWithIndex.map { (row, y) =>
          div(cls := "board-row")(
            row.zipWithIndex.map { (cell, x) =>
              cellButton(cell, x, y, isMyTurn)
            }*
          )
        }*
      )
    )

  private def cellButton(cell: Cell, x: Int, y: Int, isMyTurn: Boolean): Html[Msg] = {
    val isEmpty = cell.mark.isEmpty
    val clickable = isEmpty && isMyTurn
    
    button(
      cls := s"cell nes-btn ${if clickable then "is-primary" else ""} ${if !isEmpty then "occupied" else ""}",
      onClick(if clickable then Msg.MakeMove(x, y) else Msg.NoOp),
      disabled(!clickable)
    )(
      cell.mark.map(_.toString).getOrElse(" ")
    )
  }
} 