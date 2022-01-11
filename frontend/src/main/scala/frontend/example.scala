package frontend

import com.raquo.airstream.core.EventStream
import com.raquo.airstream.state.Var
import org.scalajs.dom.window.alert
import org.scalajs.dom.{KeyCode, console}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.scalajs.js.timers.setTimeout

object example:

  private val state = State()

  def view =
    Element.of(
      Element.of("+").onClick(state.incrementClicks),
      Element.of(state.clicks),
      Element.of("-").onClick(state.decrementClicks),
      Element
        .of(state.text)
        .onInput(state.setText)
        .placeholder("El nombre de tu perri")
        .onKeyPress { case KeyCode.Enter => state.addPerri },
      Element.of(state.error),
      Element.ofMany(state.names.map(_.map(Perri))),
    )

  private def Perri(name: String) =
    Element.of(
      Element.of("-").onClick(state.removePerri(name)),
      Element.of(s"el perri se llama $name"),
    )

  case class State(
    private val _clicks: Var[Int] = Var(0),
    private val _text: Var[String] = Var(""),
    private val _names: Var[List[String]] = Var("Nube" :: "Canela" :: "Rocco" :: Nil),
    private val _error: Var[String] = Var(""),
  ):

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

    def clicks = _clicks.signal
    def text   = _text.signal
    def names  = _names.signal
    def error  = _error.signal

    private def dispatch(info: String = "")(f: => Any): Unit = asynchronously(debug(this)(info)(f))

    override def toString: String =
      s"""|state:
          |  clicks: ${clicks.now()}
          |  text: ${text.now()}
          |  names: ${names.now().mkString("[", ", ", "]")}""".stripMargin
