package io.scrabit.example.tictactoe.indigo.network

import indigo.*

sealed trait GameNetEvent extends GlobalEvent

case class SendWs(data: String) extends GameNetEvent
