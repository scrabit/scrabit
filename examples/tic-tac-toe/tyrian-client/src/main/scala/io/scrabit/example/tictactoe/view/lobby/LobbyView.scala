package io.scrabit.example.tictactoe.view.lobby

import tyrian.Html
import tyrian.Html.*
import io.scrabit.example.tictactoe.state.State
import io.scrabit.example.tictactoe.state.game.*
import io.scrabit.example.tictactoe.msg.Msg
import io.scrabit.example.tictactoe.msg.lobby.LobbyMsg
import io.scrabit.example.tictactoe.view.components.ConnectionIndicator
import org.scalajs.dom.KeyboardEvent

object LobbyView:

  def apply(state: State, lobby: Lobby): Html[Msg] =
    div(cls := "lobby")(
      div(cls := "room-section")(
        div(cls := "lobby-header")(
          h2(s"Welcome, ${state.username}!"),
          ConnectionIndicator(state.connection)
        ),
        h3("Game Lobby"),
        roomList(lobby.rooms),
        createRoomForm(lobby.newRoomName)
      )
    )

  private def roomList(rooms: List[Room]): Html[Msg] =
    if rooms.isEmpty then
      div(cls := "no-rooms nes-container")(
        i(cls := "nes-icon trophy is-large")(),
        p("No rooms yet. Create one to get started!")
      )
    else
      div(cls := "room-list")(
        rooms.map(roomItem)
      )

  private def roomItem(room: Room): Html[Msg] =
    div(cls := "room-item nes-container")(
      h4(room.name),
      div(cls := "room-meta")(
        span()(s"Host: ${room.owner}")
      ),
      div(cls := "lobby-controls")(
        button(
          cls := "nes-btn is-primary",
          onClick(Msg.Lobby(LobbyMsg.JoinRoom(room.id)))
        )("Join")
      )
    )

  private def createRoomForm(newRoomName: String): Html[Msg] =
    div(cls := "create-room-section")(
      div(cls := "create-room-form nes-container with-title")(
        h3(cls := "title")("Create Room"),
        div(cls := "nes-field")(
          label(htmlFor := "roomName")("Room Name:"),
          input(
            id          := "roomName",
            cls         := "nes-input",
            placeholder := "Enter room name...",
            value       := newRoomName,
            onInput(name => Msg.Lobby(LobbyMsg.UpdateNewRoomName(name))),
            onEvent[KeyboardEvent, Msg](
              "keyup",
              (ke: KeyboardEvent) =>
                if ke.key == "Enter" && newRoomName.trim.nonEmpty then Msg.Lobby(LobbyMsg.CreateRoom)
                else Msg.NoOp
            )
          )
        ),
        div(cls := "lobby-controls")(
          button(
            cls := "nes-btn is-success",
            onClick(Msg.Lobby(LobbyMsg.CreateRoom)),
            disabled(newRoomName.trim.isEmpty)
          )("Create Room")
        )
      )
    )
