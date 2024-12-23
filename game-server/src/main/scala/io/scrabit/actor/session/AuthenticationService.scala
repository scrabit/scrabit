package io.scrabit.actor.session

import org.apache.pekko.actor.typed.receptionist.ServiceKey
import org.apache.pekko.actor.typed.ActorRef
import io.scrabit.actor.CommunicationHub
import org.apache.pekko.http.scaladsl.model.ws.Message
import io.scrabit.actor.message.OutgoingMessage

object AuthenticationService:

  case class Login(userId: String, password: String, connection: ActorRef[OutgoingMessage], replyTo: ActorRef[CommunicationHub.SessionCreated])
  val AuthenticationServiceKey = ServiceKey[Login]("authentication-service")
