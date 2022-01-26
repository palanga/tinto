package frontend

import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.airstream.state.{StrictSignal, Var}
import mira.*
import mira.Shape.when
import org.scalajs.dom.window.alert
import org.scalajs.dom.{KeyCode, console}
import zio.ZIO

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.scalajs.js.timers.setTimeout

object example:

  private val state = State()

  def root: Shape[zio.ZEnv] =
    Navigation
      ++ Perris.showWhen(state.selectedTab.map(_ == Tab.Perris))
//      ++ Counter.showWhen(state.selectedTab.map(_ == Tab.Clicks))
      ++ when(state.selectedTab.map(_ != Tab.Clicks)).hide(Counter)
//    Shape.list(
////      Element.of("fetch").onClick(fetchArticles),
//      Navigation,
//      Perris.when(state.selectedTab.map(_ == Tab.Perris)),
//      Counter.when(state.selectedTab.map(_ == Tab.Clicks)),
//    )

  import zio.duration.*

  val a: Edge[zio.console.Console & zio.clock.Clock] = Shape.empty :: Navigation

  val a1: Edge[zio.console.Console & zio.clock.Clock] = Shape.empty +: Navigation

  val b: Edge[zio.console.Console & zio.clock.Clock] = Navigation ++ Perris

  private def Navigation =
    Shape.text("perris").onClick_(state.showPerris) ++ Shape.text("clicks").onClick_(state.showClicks)

  private def Perris =
    Shape
      .input
      .text(state.text)
      .onInput_(state.setText)
      .placeholder("El nombre de tu perri")
      .onKeyPress { case KeyCode.Enter => state.addPerriZIO }
      ++ Shape.text(state.error).showWhen(state.error.map(_.nonEmpty))
      ++ Shape.list(state.names.map(_.map(Perri)))

  private def Perri(name: String) =
    Shape.text("-").onClick_(state.removePerri(name)) ++ Shape.text(s"el perri se llama $name")

  private def Counter =
    Shape.text("+").onClick_(state.incrementClicks)
      ++ Shape.text(state.clicks)
      ++ Shape.text("-").onClick_(state.decrementClicks)

  case class State(
    private val _selectedTab: Var[Tab] = Var(Tab.Perris),
    private val _clicks: Var[Int] = Var(0),
    private val _text: Var[String] = Var(""),
    private val _names: Var[List[String]] = Var("Nube" :: "Canela" :: "Rocco" :: Nil),
    private val _error: Var[String] = Var(""),
  ):

    def showPerris = dispatch_("show perris")(_selectedTab.set(Tab.Perris))
    def showClicks = dispatch_("show clicks")(_selectedTab.set(Tab.Clicks))

    def incrementClicks = dispatch_("increment clicks")(_clicks.update(_ + 1))

    def decrementClicks = dispatch_("decrement clicks")(_clicks.update(_ - 1))

    def setText(text: String) = dispatch_(s"set text: $text")(_text.set(text))

    def addPerri = dispatch_("add perri") {
      val name = _text.now()
      if !name.isBlank then
        _names.update(name :: _)
        _text.set("")
        _error.set("")
      else
        val message = "El nombre del perro no puede ser vacío"
        dispatch_(s"error: $message")(_error.set(message))
    }

    def addPerriZIO = dispatch("add perri") {
      val name = _text.now()
      if !name.isBlank then
        ZIO succeed {
          _names.update(name :: _)
          _text.set("")
          _error.set("")
        }
      else
        val message = "El nombre del perro no puede ser vacío"
        dispatch(s"error: $message")(ZIO succeed _error.set(message))
    }

    def removePerri(name: String) = dispatch_(s"remove perri: $name")(_names.update(_.filterNot(_ == name)))

    private def dispatch_(info: String = "")(f: => Any) = asynchronously(debug(this)(info)(f))

    private def dispatch(info: String = "")(zio: ZIO[Any, Nothing, Any]): ZIO[Any, Nothing, Unit] =
      debugZIO(this)(info)(zio).forkDaemon.unit

    private def asynchronouslyZIO(zio: ZIO[Any, Nothing, Any]) = zio.forkDaemon

    private def debugZIO(state: Any)(info: String)(zio: ZIO[Any, Nothing, Any]): ZIO[Any, Nothing, Unit] =
      groupLog(info)
        *> debugLog("prev " + state.toString)
        *> zio
        *> debugLog("next " + state.toString)
        *> groupLogEnd

    private def groupLog(input: String) = ZIO succeed console.group(input)
    private def groupLogEnd             = ZIO succeed console.groupEnd()
    private def debugLog(input: String) = ZIO succeed console.debug(input)

//  console.group(info)
//  console.debug("prev " + state.toString)
//  f
//  console.debug("next " + state.toString)
//  console.groupEnd()

    def selectedTab = _selectedTab.signal

    def error = _error.signal

    override def toString: String =
      s"""|state:
          |  clicks: ${clicks.now()}
          |  text: ${text.now()}
          |  names: ${names.now().mkString("[", ", ", "]")}""".stripMargin

    def clicks = _clicks.signal

    def text = _text.signal

    def names = _names.signal

  enum Tab:
    case Perris, Clicks
