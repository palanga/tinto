package mira

import com.raquo.domtypes.generic.Modifier
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import zio.{Runtime, ZIO}

sealed trait Shape(attributes: List[Attribute]):

  def onClick(zio: ZIO[Any, Nothing, Any]): Shape = this.addAttribute(Attribute.OnClick(zio))
  def onClick(f: => Unit): Shape                  = this.addAttribute(Attribute.OnClick(ZIO succeed f))
  def when(condition: Signal[Boolean]): Shape.Edge
  def build(using runtime: Runtime[Any]): LaminarElem

  protected def addAttribute(attribute: Attribute): Shape

object Shape:
  val empty: Node                               = Node("")
  val input: Node                               = Node("", kind = "input")
  def text(text: String | AnyVal): Node         = Node(text.toString)
  def text(text: Signal[String | AnyVal]): Node = empty.bind(text)
  def list(shapes: Signal[List[Shape]]): Node   = empty.bindAll(shapes) // TODO return edge ?
  def list(shape: Shape, shapes: Shape*): Edge  = Edge(shapes.prepended(shape))

  case class Node(
    private val text: String,
    private val attributes: List[Attribute] = Nil,
    private val kind: "div" | "input" = "div",
  ) extends Shape(attributes):

    def bind(signal: Signal[Any]): Node = this.addAttribute(Attribute.BindSignal(signal))

    def bindAll(signal: Signal[Seq[Shape]]): Node = this.addAttribute(Attribute.BindSignals(signal))

    def placeholder(text: String): Node = this.addAttribute(Attribute.Placeholder(text))

    def onInput(f: String => Unit): Node = this.copy(attributes = Attribute.OnInput(f) :: attributes, kind = "input")

    def onKeyPress(f: KeyCode => Unit): Node = this.addAttribute(Attribute.OnKeyPress(f))

    def onKeyPress(f: PartialFunction[KeyCode, Unit]): Node = this.addAttribute(Attribute.OnKeyPress(f orElse noop))

    override def when(condition: Signal[Boolean]): Edge = Edge(Seq(this), conditionalShow = Some(condition))

    override def build(using runtime: Runtime[Any]): LaminarElem =
      val laminarMods = attributes.map(_.toLaminarModFor(this))
      this.kind match {
        case "div"   => L.div(text, laminarMods)
        case "input" => L.input(laminarMods)
      }

    override def addAttribute(attribute: Attribute): Node = this.copy(attributes = attribute :: attributes)

  case class Edge(
    private val children: Seq[Shape],
    private val attributes: List[Attribute] = Nil,
    private val conditionalShow: Option[Signal[Boolean]] = None,
  ) extends Shape(attributes):

    override def when(condition: Signal[Boolean]): Edge = this.copy(conditionalShow = Some(condition))

    override def build(using runtime: Runtime[Any]): LaminarElem =
      val childNode = L.div(children.map(_.build), attributes.map(_.toLaminarModFor(this)))
      conditionalShow.fold(childNode)(shouldDisplay =>
        L.div(L.child.maybe <-- shouldDisplay.map(Option.when(_)(childNode)))
      )

    override def addAttribute(attribute: Attribute): Shape = this.copy(attributes = attribute :: attributes)

private[mira] val none: Val[None.type]             = Val(None)
private[mira] val always: Val[Boolean]             = Val(true)
private[mira] val noop: PartialFunction[Any, Unit] = _ => ()

private[mira] type LaminarElem = ReactiveHtmlElement[_ <: dom.html.Element]
private[mira] type LaminarMod  = Modifier[LaminarElem]
type KeyCode                   = Int
