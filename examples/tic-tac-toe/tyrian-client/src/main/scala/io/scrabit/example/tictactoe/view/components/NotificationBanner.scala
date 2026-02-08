package io.scrabit.example.tictactoe.view.components

import tyrian.Html
import tyrian.Html.*
import io.scrabit.example.tictactoe.state.State
import io.scrabit.example.tictactoe.state.ui.{Notification, NotificationLevel}
import io.scrabit.example.tictactoe.msg.Msg
import io.scrabit.example.tictactoe.msg.ui.UiMsg

object NotificationBanner:

  def apply(state: State): Html[Msg] =
    state.notification match
      case None => div()
      case Some(notification) =>
        val levelCls = notification.level match
          case NotificationLevel.Error   => "is-error"
          case NotificationLevel.Success => "is-success"
          case NotificationLevel.Info    => "is-info"

        div(
          cls := s"notification-banner $levelCls",
          onClick(Msg.Ui(UiMsg.DismissNotification))
        )(
          span()(notification.message),
          span(cls := "notification-dismiss")("x")
        )
