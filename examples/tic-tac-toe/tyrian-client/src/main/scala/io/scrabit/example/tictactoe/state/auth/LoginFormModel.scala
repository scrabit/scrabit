package  io.scrabit.example.tictactoe.state.auth

case class LoginFormModel(username: String, password: String)

object LoginFormModel {
  val empty: LoginFormModel = LoginFormModel("", "")
}
