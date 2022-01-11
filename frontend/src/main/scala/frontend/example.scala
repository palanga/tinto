package frontend

import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.airstream.state.{StrictSignal, Var}
import org.scalajs.dom.window.alert
import org.scalajs.dom.{KeyCode, console}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.scalajs.js.timers.setTimeout

object example:

  private val state = State()

  def root: Element =
    Element.of(
      Navigation,
      Perris.when(state.selectedTab.map(_ == Tab.Perris)),
      Counter.when(state.selectedTab.map(_ == Tab.Clicks)),
    )

  private def Perri(name: String) =
    Element.of(
      Element.of("-").onClick(state.removePerri(name)),
      Element.of(s"el perri se llama $name"),
    )

  private def Navigation: Element =
    Element.of(
      Element.of("perris").onClick(state.showPerris),
      Element.of("clicks").onClick(state.showClicks),
    )

  private def Perris: Element =
    Element.of(
      Element
        .of(state.text)
        .onInput(state.setText)
        .placeholder("El nombre de tu perri")
        .onKeyPress { case KeyCode.Enter => state.addPerri },
      Element.of(state.error).when(state.error.map(_.nonEmpty)),
      Element.ofMany(state.names.map(_.map(Perri))),
    )

  private def Counter: Element =
    Element.of(
      Element.of("+").onClick(state.incrementClicks),
      Element.of(state.clicks),
      Element.of("-").onClick(state.decrementClicks),
    )

  enum Tab:
    case Perris, Clicks

  case class State(
    private val _selectedTab: Var[Tab] = Var(Tab.Perris),
    private val _clicks: Var[Int] = Var(0),
    private val _text: Var[String] = Var(""),
    private val _names: Var[List[String]] = Var("Nube" :: "Canela" :: "Rocco" :: Nil),
    private val _error: Var[String] = Var(""),
  ):

    def showPerris = dispatch("show perris")(_selectedTab.set(Tab.Perris))
    def showClicks = dispatch("show clicks")(_selectedTab.set(Tab.Clicks))

    def incrementClicks = dispatch("increment clicks")(_clicks.update(_ + 1))

    def decrementClicks = dispatch("decrement clicks")(_clicks.update(_ - 1))

    def setText(text: String) = dispatch(s"set text: $text")(_text.set(text))

    def addPerri = dispatch("add perri") {
      val name = _text.now()
      if !name.isBlank then
        _names.update(name :: _)
        _text.set("")
        _error.set("")
      else
        val message = "El nombre del perro no puede ser vacío"
        dispatch(s"error: $message")(_error.set(message))
    }

    def removePerri(name: String) = dispatch(s"remove perri: $name")(_names.update(_.filterNot(_ == name)))

    def selectedTab = _selectedTab.signal
    def clicks      = _clicks.signal
    def text        = _text.signal
    def names       = _names.signal
    def error       = _error.signal

    private def dispatch(info: String = "")(f: => Any): Unit = asynchronously(debug(this)(info)(f))

    override def toString: String =
      s"""|state:
          |  clicks: ${clicks.now()}
          |  text: ${text.now()}
          |  names: ${names.now().mkString("[", ", ", "]")}""".stripMargin
