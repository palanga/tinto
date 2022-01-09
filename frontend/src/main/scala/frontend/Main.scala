package frontend

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.{Var, documentEvents, render, unsafeWindowOwner}
import org.scalajs.dom
import org.scalajs.dom.window.alert

object Main:

  def main(args: Array[String]): Unit = {
    val _ = documentEvents.onDomContentLoaded.foreach { _ =>
      val appContainer = dom.document.querySelector("#app")
      appContainer.innerHTML = ""
      val _ = render(appContainer, view.build)
    }(unsafeWindowOwner)
  }

  private val state = State()

  def view =
    Element.of(
      Element.of("holanda").onClick(alert("co tas")),
      Element.of("+").onClick(state.incrementClicks),
      Element.of(state.clicks),
      Element.of("-").onClick(state.decrementClicks),
      Element.of(state.text).onInput(state.setText),
      Element.of("agregar").onClick(state.addPerri),
      Element.ofMany(state.names.map(_.map(perri))),
    )

  def perri(name: String) =
    Element.of(
      Element.of("-").onClick(state.removePerri(name)),
      Element.of(s"el perri se llama $name"),
    )

  case class State(
    private val _clicks: Var[Int] = Var(0),
    private val _text: Var[String] = Var("Poné el nombre de un perri"),
    private val _names: Var[List[String]] = Var("Nube" :: "Canela" :: "Rocco" :: Nil),
  ):

    def incrementClicks           = dispatch(Command.IncrementClicks)
    def decrementClicks           = dispatch(Command.DecrementClicks)
    def setText(text: String)     = dispatch(Command.SetText(text))
    def addPerri                  = dispatch(Command.AddPerri)
    def removePerri(name: String) = dispatch(Command.RemovePerri(name))

    def clicks = _clicks.signal
    def text   = _text.signal
    def names  = _names.signal

    private def dispatch = debugWrapper(commandHandler)

    private def commandHandler(command: Command): Unit =
      command match {
        case Command.IncrementClicks   => _clicks.update(_ + 1)
        case Command.DecrementClicks   => _clicks.update(_ - 1)
        case Command.SetText(text)     => _text.set(text)
        case Command.AddPerri          => _names.update(_text.now() :: _); _text.set("")
        case Command.RemovePerri(name) => _names.update(_.filterNot(_ == name))

      }

    private def debugWrapper[T](f: T => Any)(command: T): Unit =
      dom.console.group("State dispatcher debug")
      dom.console.debug("prev " + state.toString)
      dom.console.debug("command: " + command.toString)
      f(command)
      dom.console.debug("post " + state.toString)
      dom.console.groupEnd()

    override def toString: String =
      s"""|state:
          |  clicks: ${clicks.now()}
          |  text: ${text.now()}
          |  names: ${names.now().mkString("[", ", ", "]")}""".stripMargin

  enum Command:
    case IncrementClicks
    case DecrementClicks
    case SetText(text: String)
    case AddPerri
    case RemovePerri(name: String)
