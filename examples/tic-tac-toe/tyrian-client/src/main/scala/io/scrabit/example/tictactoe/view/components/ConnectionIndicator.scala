package io.scrabit.example.tictactoe.view.components

import tyrian.Html
import tyrian.Html.*
import io.scrabit.example.tictactoe.state.connection.ConnectionStatus
import io.scrabit.example.tictactoe.msg.Msg

object ConnectionIndicator:

  def apply(connection: ConnectionStatus): Html[Msg] =
    val (dotCls, label) = connection match
      case ConnectionStatus.Connected(_)       => ("connection-dot connected", "Connected")
      case ConnectionStatus.Connecting(_)      => ("connection-dot connecting", "Connecting...")
      case ConnectionStatus.ConnectionError(_) => ("connection-dot disconnected", "Error")
      case ConnectionStatus.Disconnecting      => ("connection-dot disconnected", "Disconnecting...")
      case ConnectionStatus.Disconnected       => ("connection-dot disconnected", "Disconnected")

    div(cls := "connection-indicator")(
      span(cls := dotCls)(),
      span()(label)
    )
