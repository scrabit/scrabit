package io.scrabit.example.tictactoe.state.ui

enum NotificationLevel:
  case Info, Success, Error

case class Notification(message: String, level: NotificationLevel, ttl: Int = 3)
