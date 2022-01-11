package frontend

import com.raquo.domtypes.generic.Modifier
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.html

sealed trait Element(attributes: List[Attribute]):
  def onClick(f: => Any): Element = this.addAttribute(Attribute.OnClick(() => f))
  def when(condition: Signal[Boolean]): Element.Edge
  def build: LaminarElem
  protected def addAttribute(attribute: Attribute): Element

object Element:
  val empty: Node                                  = Node("")
  val input: Node                                  = Node("", kind = "input")
  def of(text: String): Node                       = Node(text)
  def ofMany(signal: Signal[List[Element]]): Node  = empty.bindAll(signal) // TODO return edge ?
  def of(signal: Signal[Any]): Node                = empty.bind(signal)
  def of(child: Element, children: Element*): Edge = Edge(child, children)

  case class Node(
    private val text: String,
    private val attributes: List[Attribute] = Nil,
    private val kind: "div" | "input" = "div",
  ) extends Element(attributes):

    def when(condition: Signal[Boolean]): Edge = Edge(this, conditionalShow = Some(condition))

    def bindAll(signal: Signal[Seq[Element]]): Node = this.addAttribute(Attribute.BindSignals(signal))

    def bind(signal: Signal[Any]): Node = this.addAttribute(Attribute.BindSignal(signal))

    def onInput(f: String => Any): Node = this.copy(attributes = Attribute.OnInput(f) :: attributes, kind = "input")

    def placeholder(text: String): Node = this.addAttribute(Attribute.Placeholder(text))

    def onKeyPress(f: KeyCode => Any): Node                = this.addAttribute(Attribute.OnKeyPress(f))
    def onKeyPress(f: PartialFunction[KeyCode, Any]): Node = this.addAttribute(Attribute.OnKeyPress(f orElse noop))

    override def build: LaminarElem =
      val laminarMods = attributes.map(_.toLaminarModFor(this))
      this.kind match {
        case "div"   => L.div(text, laminarMods)
        case "input" => L.input(laminarMods)
      }

    override def addAttribute(attribute: Attribute): Node = this.copy(attributes = attribute :: attributes)

  case class Edge(
    private val child: Element,
    private val children: Seq[Element] = Nil,
    private val attributes: List[Attribute] = Nil,
    private val conditionalShow: Option[Signal[Boolean]] = None,
  ) extends Element(attributes):

    def when(condition: Signal[Boolean]): Edge = this.copy(conditionalShow = Some(condition))

    override def build: LaminarElem =
      val childNode = L.div(child.build, children.toList.map(_.build), attributes.map(_.toLaminarModFor(this)))
      conditionalShow.fold(childNode)(shouldDisplay =>
        L.div(L.child.maybe <-- shouldDisplay.map(if _ then Some(childNode) else None))
      )

    override def addAttribute(attribute: Attribute): Element = this.copy(attributes = attribute :: attributes)

sealed trait Attribute:
  def toLaminarModFor(elem: Element): LaminarMod

object Attribute:

  case class BindSignal(signal: Signal[Any]) extends Attribute:
    def toLaminarModFor(elem: Element): LaminarMod = elem match {
      case Element.Node(_, _, "input") => L.value <-- signal.map(_.toString)
      case _                           => L.child.text <-- signal.map(_.toString) // TODO ??? deah
    }

  case class BindSignals(signal: Signal[Seq[Element]]) extends Attribute:
    def toLaminarModFor(elem: Element): LaminarMod = L.children <-- signal.map(_.map(_.build))

  case class OnClick(f: () => Any) extends Attribute:
    def toLaminarModFor(elem: Element): LaminarMod = L.onClick --> { _ => f() }

  case class OnInput(f: String => Any) extends Attribute:
    def toLaminarModFor(elem: Element): LaminarMod = L.onInput.mapToValue --> { f(_) }

  case class OnKeyPress(f: Int => Any) extends Attribute:
    def toLaminarModFor(elem: Element): LaminarMod = L.onKeyPress.map(_.keyCode) --> { f(_) }

  case class Placeholder(text: String) extends Attribute:
    def toLaminarModFor(elem: Element): LaminarMod = elem match {
      case Element.Node(_, _, "input") => L.placeholder := text
      case _                           => L.emptyMod // TODO should not happen
    }

//  case class When(condition: Signal[Boolean]) extends Attribute:
//    override def toLaminarModFor(elem: Element): LaminarMod = L.child.maybe <-- condition

private val none: Val[None.type] = Val(None)
private val always: Val[Boolean] = Val(true)

private type LaminarElem = ReactiveHtmlElement[_ <: dom.html.Element]
private type LaminarMod  = Modifier[LaminarElem]
type KeyCode             = Int

private val noop: PartialFunction[Any, Unit] = _ => ()
