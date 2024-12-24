package io.scrabit.actor.session

import org.apache.pekko.actor.typed.receptionist.ServiceKey
import org.apache.pekko.actor.typed.ActorRef
import io.scrabit.actor.CommunicationHub
import org.apache.pekko.http.scaladsl.model.ws.Message
import io.scrabit.actor.message.OutgoingMessage

import java.security.SecureRandom
import java.util.Base64

object AuthenticationService:

  case class Login(userId: String, password: String, connection: ActorRef[OutgoingMessage], replyTo: ActorRef[CommunicationHub.SessionCreated])
  val AuthenticationServiceKey: ServiceKey[Login] = ServiceKey[Login]("authentication-service")
  
  def generateSessionKey(): String = {
    val random = new SecureRandom()
    val bytes = new Array[Byte](32) // 256-bit key
    random.nextBytes(bytes)
    Base64.getUrlEncoder.withoutPadding.encodeToString(bytes)
  }
