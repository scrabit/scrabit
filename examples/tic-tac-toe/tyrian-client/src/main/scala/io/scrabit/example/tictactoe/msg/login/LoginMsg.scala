package io.scrabit.example.tictactoe.msg.login

enum  LoginMsg {
  case Submit
  case UpdateForm(username: String, password: String)
}
