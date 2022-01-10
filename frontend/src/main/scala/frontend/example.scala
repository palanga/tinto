package frontend

import com.raquo.airstream.core.EventStream
import com.raquo.airstream.state.Var
import org.scalajs.dom.window.alert
import org.scalajs.dom.{KeyCode, console}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object example:

  private val state = State()

  def view =
    Element.of(
      Element.of("holanda").onClick(alert("co tas")),
      Element.of("+").onClick(state.incrementClicks),
      Element.of(state.clicks),
      Element.of("-").onClick(state.decrementClicks),
      Element
        .of(state.text)
        .onInput(state.setText)
        .placeholder("El nombre de tu perri")
        .onKeyPress { case KeyCode.Enter => state.addPerri },
      Element.of("agregar").onClick(state.addPerri),
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
  ):

    def incrementClicks           = dispatch(Command.IncrementClicks)
    def decrementClicks           = dispatch(Command.DecrementClicks)
    def setText(text: String)     = dispatch(Command.SetText(text))
    def addPerri                  = dispatch(Command.AddPerri)
    def removePerri(name: String) = dispatch(Command.RemovePerri(name))

    def clicks = _clicks.signal
    def text   = _text.signal
    def names  = _names.signal

    private val debounceWrapper            = DebounceWrapper.init
    private def dispatch(command: Command) = debounceWrapper(debugWrapper(commandHandler))(command)

    private def commandHandler(command: Command): Unit =
      command match {
        case Command.IncrementClicks => _clicks.update(_ + 1)
        case Command.DecrementClicks => _clicks.update(_ - 1)
        case Command.SetText(text)   => _text.set(text)
        case Command.AddPerri =>
          val name = _text.now()
          if !name.isBlank then
            _names.update(name :: _)
            _text.set("")
          else dispatch(Command.Error(s"El nombre del perro no puede ser vacío"))
        case Command.RemovePerri(name) => _names.update(_.filterNot(_ == name))
        case Command.Error(message)    => alert(message)
      }

    case class DebounceWrapper private ():
      private val queue = scala.collection.mutable.Queue.empty[Any => Unit]
      scala.scalajs.js.timers.setInterval(8 milliseconds)(if queue.nonEmpty then queue.dequeue()(()))
      def handle[T](f: T => Any)(command: T): Unit = queue.enqueue(_ => f(command))
      def apply[T](f: T => Any)(command: T): Unit  = queue.enqueue(_ => f(command))

    object DebounceWrapper:
      def init: DebounceWrapper = new DebounceWrapper()

    private def debugWrapper[T](f: T => Any)(command: T): Unit =
      console.group("command: " + command.toString)
      console.debug("prev " + state.toString)
      f(command)
      console.debug("post " + state.toString)
      console.groupEnd()

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
    case Error(message: String)
