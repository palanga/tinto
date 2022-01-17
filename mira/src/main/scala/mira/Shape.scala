package mira

import com.raquo.domtypes.generic.Modifier
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.html
import zio.Runtime

sealed trait Shape(attributes: List[Attribute]):
  def onClick(f: => Any): Shape = this.addAttribute(Attribute.OnClick(() => f))
  def when(condition: Signal[Boolean]): Shape.Edge
  def build(using runtime: Runtime[Any]): LaminarElem
  protected def addAttribute(attribute: Attribute): Shape

object Shape:
  val empty: Node                               = Node("")
  val input: Node                               = Node("", kind = "input")
  def of(text: String): Node                    = Node(text)
  def ofMany(signal: Signal[List[Shape]]): Node = empty.bindAll(signal) // TODO return edge ?
  def of(signal: Signal[Any]): Node             = empty.bind(signal)
  def of(child: Shape, children: Shape*): Edge  = Edge(children.prepended(child))

  case class Node(
    private val text: String,
    private val attributes: List[Attribute] = Nil,
    private val kind: "div" | "input" = "div",
  ) extends Shape(attributes):

    def when(condition: Signal[Boolean]): Edge = Edge(Seq(this), conditionalShow = Some(condition))

    def bindAll(signal: Signal[Seq[Shape]]): Node = this.addAttribute(Attribute.BindSignals(signal))

    def bind(signal: Signal[Any]): Node = this.addAttribute(Attribute.BindSignal(signal))

    override def addAttribute(attribute: Attribute): Node = this.copy(attributes = attribute :: attributes)

    def onInput(f: String => Any): Node = this.copy(attributes = Attribute.OnInput(f) :: attributes, kind = "input")

    def placeholder(text: String): Node = this.addAttribute(Attribute.Placeholder(text))

    def onKeyPress(f: KeyCode => Any): Node = this.addAttribute(Attribute.OnKeyPress(f))

    def onKeyPress(f: PartialFunction[KeyCode, Any]): Node = this.addAttribute(Attribute.OnKeyPress(f orElse noop))

    override def build(using runtime: Runtime[Any]): LaminarElem =
      val laminarMods = attributes.map(_.toLaminarModFor(this))
      this.kind match {
        case "div"   => L.div(text, laminarMods)
        case "input" => L.input(laminarMods)
      }

  case class Edge(
    private val children: Seq[Shape],
    private val attributes: List[Attribute] = Nil,
    private val conditionalShow: Option[Signal[Boolean]] = None,
  ) extends Shape(attributes):

    def when(condition: Signal[Boolean]): Edge = this.copy(conditionalShow = Some(condition))

    override def build(using runtime: Runtime[Any]): LaminarElem =
      val childNode = L.div(children.map(_.build), attributes.map(_.toLaminarModFor(this)))
      conditionalShow.fold(childNode)(shouldDisplay =>
        L.div(L.child.maybe <-- shouldDisplay.map(Option.when(_)(childNode)))
      )

    override def addAttribute(attribute: Attribute): Shape = this.copy(attributes = attribute :: attributes)

sealed trait Attribute:
  def toLaminarModFor(elem: Shape)(using runtime: Runtime[Any]): LaminarMod

object Attribute:

  case class BindSignal(signal: Signal[Any]) extends Attribute:
    def toLaminarModFor(elem: Shape)(using runtime: Runtime[Any]): LaminarMod = elem match {
      case Shape.Node(_, _, "input") => L.value <-- signal.map(_.toString)
      case _                         => L.child.text <-- signal.map(_.toString) // TODO ??? deah
    }

  case class BindSignals(signal: Signal[Seq[Shape]]) extends Attribute:
    def toLaminarModFor(elem: Shape)(using runtime: Runtime[Any]): LaminarMod =
      L.children <-- signal.map(_.map(_.build))

  case class OnClick(f: () => Any) extends Attribute:
    def toLaminarModFor(elem: Shape)(using runtime: Runtime[Any]): LaminarMod = L.onClick --> { _ => f() }

  case class OnInput(f: String => Any) extends Attribute:
    def toLaminarModFor(elem: Shape)(using runtime: Runtime[Any]): LaminarMod = L.onInput.mapToValue --> { f(_) }

  case class OnKeyPress(f: Int => Any) extends Attribute:
    def toLaminarModFor(elem: Shape)(using runtime: Runtime[Any]): LaminarMod = L.onKeyPress.map(_.keyCode) --> { f(_) }

  case class Placeholder(text: String) extends Attribute:
    def toLaminarModFor(elem: Shape)(using runtime: Runtime[Any]): LaminarMod = elem match {
      case Shape.Node(_, _, "input") => L.placeholder := text
      case _                         => L.emptyMod // TODO should not happen
    }

private val none: Val[None.type] = Val(None)
private val always: Val[Boolean] = Val(true)

private type LaminarElem = ReactiveHtmlElement[_ <: dom.html.Element]
private type LaminarMod  = Modifier[LaminarElem]
type KeyCode             = Int

private val noop: PartialFunction[Any, Unit] = _ => ()
