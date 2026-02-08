package io.scrabit.example.tictactoe.indigo.scenes

import indigo.*
import indigo.scenes.*
import indigo.shared.networking.WebSocketEvent

import io.scrabit.example.tictactoe.indigo.model.*
import io.scrabit.example.tictactoe.indigo.network.*

import generated.*

object LoginScene extends Scene[Unit, Model, ViewModel]:

  val name: SceneName = SceneName("Login")

  type SceneModel     = LoginModel
  type SceneViewModel = ViewModel

  val modelLens: Lens[Model, LoginModel] =
    Lens(
      _.login,
      (m, l) => m.copy(login = l)
    )

  val viewModelLens: Lens[ViewModel, ViewModel] =
    Lens.keepLatest

  val eventFilters: EventFilters = EventFilters.Permissive

  val subSystems: Set[SubSystem[Model]] = Set()

  def updateModel(
      context: SceneContext[Unit],
      sceneModel: LoginModel
  ): GlobalEvent => Outcome[LoginModel] =
    case KeyboardEvent.KeyDown(key) if key == Key.TAB =>
      Outcome(sceneModel.copy(focusedField = (sceneModel.focusedField + 1) % 2))

    case KeyboardEvent.KeyDown(key) if key == Key.ENTER =>
      if sceneModel.username.nonEmpty && sceneModel.password.nonEmpty then
        Outcome(sceneModel.copy(error = None))
          .addGlobalEvents(
            WebSocketEvent.Open(
              Protocol.mkLogin(sceneModel.username, sceneModel.password),
              ConnectionState.initial.wsConfig
            )
          )
      else
        Outcome(sceneModel.copy(error = Some("Enter username and password")))

    case KeyboardEvent.KeyDown(key) if key == Key.BACKSPACE =>
      if sceneModel.focusedField == 0 then
        Outcome(sceneModel.copy(username = sceneModel.username.dropRight(1)))
      else
        Outcome(sceneModel.copy(password = sceneModel.password.dropRight(1)))

    case e: KeyboardEvent.KeyDown if e.key.isPrintable =>
      val ch = if e.isShiftKeyDown then e.key.key.toUpperCase else e.key.key
      if sceneModel.focusedField == 0 then
        Outcome(sceneModel.copy(username = sceneModel.username + ch))
      else
        Outcome(sceneModel.copy(password = sceneModel.password + ch))

    case _ =>
      Outcome(sceneModel)

  def updateViewModel(
      context: SceneContext[Unit],
      sceneModel: LoginModel,
      sceneViewModel: ViewModel
  ): GlobalEvent => Outcome[ViewModel] =
    case FrameTick =>
      val elapsed = context.frame.time.running.toDouble
      val shouldBlink = (elapsed - sceneViewModel.lastBlink) > 0.5
      if shouldBlink then
        Outcome(sceneViewModel.copy(cursorVisible = !sceneViewModel.cursorVisible, lastBlink = elapsed))
      else
        Outcome(sceneViewModel)
    case _ =>
      Outcome(sceneViewModel)

  def present(
      context: SceneContext[Unit],
      sceneModel: LoginModel,
      sceneViewModel: ViewModel
  ): Outcome[SceneUpdateFragment] =

    val cyan  = RGBA.fromHexString("0x00d2ff")
    val coral = RGBA.fromHexString("0xff6b6b")
    val gold  = RGBA.fromHexString("0xffd93d")
    val dark  = RGBA.fromHexString("0x1a1a2e")
    val panel = RGBA.fromHexString("0x16213e")

    val centerX = 400

    val title = Text(
      "TIC TAC TOE",
      centerX - 100,
      80,
      GameFont.fontKey,
      Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(cyan)
    )

    val subtitle = Text(
      "INDIGO CLIENT",
      centerX - 90,
      110,
      GameFont.fontKey,
      Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(gold)
    )

    val usernameLabel = Text(
      "USERNAME",
      centerX - 120,
      190,
      GameFont.fontKey,
      Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(if sceneModel.focusedField == 0 then cyan else RGBA.White)
    )

    val usernameBox = Shape.Box(
      Rectangle(centerX - 120, 210, 240, 30),
      Fill.Color(panel),
      Stroke(2, if sceneModel.focusedField == 0 then cyan else RGBA.White.withAlpha(0.3))
    )

    val usernameText = Text(
      sceneModel.username + (if sceneModel.focusedField == 0 && sceneViewModel.cursorVisible then "_" else ""),
      centerX - 114,
      218,
      GameFont.fontKey,
      Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(RGBA.White)
    )

    val passwordLabel = Text(
      "PASSWORD",
      centerX - 120,
      260,
      GameFont.fontKey,
      Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(if sceneModel.focusedField == 1 then cyan else RGBA.White)
    )

    val passwordBox = Shape.Box(
      Rectangle(centerX - 120, 280, 240, 30),
      Fill.Color(panel),
      Stroke(2, if sceneModel.focusedField == 1 then cyan else RGBA.White.withAlpha(0.3))
    )

    val maskedPassword = "*" * sceneModel.password.length
    val passwordText = Text(
      maskedPassword + (if sceneModel.focusedField == 1 && sceneViewModel.cursorVisible then "_" else ""),
      centerX - 114,
      288,
      GameFont.fontKey,
      Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(RGBA.White)
    )

    val buttonColor =
      if sceneModel.username.nonEmpty && sceneModel.password.nonEmpty then cyan
      else RGBA.White.withAlpha(0.3)

    val loginButton = Shape.Box(
      Rectangle(centerX - 100, 340, 200, 36),
      Fill.Color(buttonColor.withAlpha(0.2)),
      Stroke(2, buttonColor)
    )

    val loginButtonText = Text(
      "LOGIN",
      centerX - 36,
      350,
      GameFont.fontKey,
      Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(buttonColor)
    )

    val hint = Text(
      "TAB: switch  ENTER: login",
      centerX - 160,
      400,
      GameFont.fontKey,
      Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(RGBA.White.withAlpha(0.4))
    )

    val errorNodes: Batch[SceneNode] = sceneModel.error match
      case Some(err) =>
        Batch(
          Text(
            err,
            centerX - 120,
            440,
            GameFont.fontKey,
            Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(coral)
          )
        )
      case None => Batch.empty

    Outcome(
      SceneUpdateFragment(
        Batch(
          title,
          subtitle,
          usernameLabel,
          usernameBox,
          usernameText,
          passwordLabel,
          passwordBox,
          passwordText,
          loginButton,
          loginButtonText,
          hint
        ) ++ errorNodes
      )
    )
