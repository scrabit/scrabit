package io.scrabit.example.tictactoe.indigo.model

import indigo.shared.networking.WebSocketConfig
import indigo.shared.networking.WebSocketId

case class ConnectionState(
    wsConfig: WebSocketConfig,
    connected: Boolean,
    sessionKey: Option[String],
    username: String
)

object ConnectionState:
  val initial: ConnectionState = ConnectionState(
    wsConfig = WebSocketConfig(WebSocketId("game"), "ws://localhost:8080"),
    connected = false,
    sessionKey = None,
    username = ""
  )
