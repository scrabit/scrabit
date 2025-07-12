import io.circe.*
import io.circe.parser.*
import io.circe.generic.auto.*
import example.ServerMessage

val text = """{"sessionKey":"testkey-scrabit"}"""
val x    = decode[ServerMessage.LoginSuccess](text).swap.map(e => e.getMessage())
