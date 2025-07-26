package io.scrabit.example.tictactoe.view

import tyrian.Html
import tyrian.Html.*
import io.scrabit.example.tictactoe.model.State.*
import io.scrabit.example.tictactoe.model.{Msg, State}

object LobbyView {

  def apply(state: State): Html[Msg] = 
    div(cls := "lobby")(
      h2(s"Welcome ${state.loginState.username}!"),
      
      div(cls := "room-section")(
        h3("Available Rooms"),
        roomList(state.lobby.map(_.rooms).getOrElse(Nil)),
        
        div(cls := "create-room-section")(
          h4("Create New Room"),
          createRoomForm(state)
        )
      ),
      
      div(cls := "lobby-controls")(
        button(cls := "nes-btn", onClick(Msg.WebSocketStatus(io.scrabit.example.tictactoe.model.Socket.Status.Disconnecting)))("Logout")
      )
    )

  private def roomList(rooms: List[Room]): Html[Msg] =
    if (rooms.isEmpty) {
      div(cls := "no-rooms")(
        p("No rooms available. Create one below!")
      )
    } else {
      div(cls := "room-list")(
        rooms.map { room =>
          div(cls := "room-item nes-container with-title")(
            h4(cls := "title")(s"Room: ${room.name}"),
            p(s"Owner: ${room.owner}"),
            button(cls := "nes-btn is-primary", onClick(Msg.JoinSpecificRoom(room.id)))(s"Join ${room.name}")
          )
        }*
      )
    }

  private def createRoomForm(state: State): Html[Msg] =
    div(cls := "create-room-form nes-container")(
      div(cls := "nes-field")(
        label(htmlFor := "room-name")("Room Name:"),
        input(
          id := "room-name",
          cls := "nes-input",
          placeholder := "Enter room name...",
          value := state.lobby.map(_.newRoomName).getOrElse(""),
          onInput(roomName => Msg.UpdateNewRoomName(roomName))
        )
      ),
      div(cls := "create-room-buttons")(
        button(
          cls := "nes-btn is-primary", 
          onClick(Msg.CreateRoom(state.lobby.map(_.newRoomName).getOrElse(""))),
          disabled(state.lobby.map(_.newRoomName.trim.isEmpty).getOrElse(true))
        )("Create Room"),
        button(cls := "nes-btn is-success", onClick(Msg.CreateRoom("Quick Game")))("Create Quick Game")
      )
    )
} 