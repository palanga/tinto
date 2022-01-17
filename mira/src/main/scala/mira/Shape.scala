package mira

import com.raquo.domtypes.generic.Modifier
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import zio.{Runtime, ZIO}

sealed trait Shape[-R](attributes: List[Attribute[R]]):

  def onClick[R1](zio: ZIO[R1, Nothing, Any]): Shape[R & R1] = this.addAttribute(Attribute.OnClick(zio))
  def onClick(f: => Unit): Shape[R]                          = this.addAttribute(Attribute.OnClick(ZIO succeed f))
  def when(condition: Signal[Boolean]): Shape.Edge[R]
  def build(using runtime: Runtime[R]): LaminarElem

  protected def addAttribute[R1](attribute: Attribute[R1]): Shape[R & R1]

object Shape:
  val empty: Node[Any]                                        = Node("")
  val input: Node[Any]                                        = Node("", kind = "input")
  def text(text: String | AnyVal): Node[Any]                  = Node(text.toString)
  def text(text: Signal[String | AnyVal]): Node[Any]          = empty.bind(text)
  def list(shapes: Signal[List[Shape[Any]]]): Node[Any]       = empty.bindAll(shapes) // TODO return edge ?
  def list(shape: Shape[Any], shapes: Shape[Any]*): Edge[Any] = Edge(shapes.prepended(shape))

  case class Node[-R](
    private val text: String,
    private val attributes: List[Attribute[R]] = Nil,
    private val kind: "div" | "input" = "div",
  ) extends Shape(attributes):

    def bind(signal: Signal[String | AnyVal]): Node[R] = this.addAttribute(Attribute.BindSignal(signal.map(_.toString)))

    def bindAll(signal: Signal[Seq[Shape[Any]]]): Node[R] = this.addAttribute(Attribute.BindSignals(signal))

    def placeholder(text: String): Node[R] = this.addAttribute(Attribute.Placeholder(text))

    def onInput(f: String => Unit): Node[R] = this.copy(kind = "input").addAttribute(Attribute.OnInput(f))

    def onKeyPress(f: KeyCode => Unit): Node[R] = this.addAttribute(Attribute.OnKeyPress(f))

    def onKeyPress(f: PartialFunction[KeyCode, Unit]): Node[R] = this.addAttribute(Attribute.OnKeyPress(f orElse noop))

    override def when(condition: Signal[Boolean]): Edge[R] = Edge(Seq(this), conditionalShow = Some(condition))

    override def build(using runtime: Runtime[R]): LaminarElem =
      val laminarMods = attributes.map(toLaminarMod(this))
      this.kind match {
        case "div"   => L.div(text, laminarMods)
        case "input" => L.input(laminarMods)
      }

    override def addAttribute[R1](attribute: Attribute[R1]): Node[R & R1] =
      this.copy(attributes = attribute :: attributes)

  case class Edge[-R](
    private val children: Seq[Shape[R]],
    private val attributes: List[Attribute[R]] = Nil,
    private val conditionalShow: Option[Signal[Boolean]] = None,
  ) extends Shape(attributes):

    override def when(condition: Signal[Boolean]): Edge[R] = this.copy(conditionalShow = Some(condition))

    override def build(using runtime: Runtime[R]): LaminarElem =
      val childNode = L.div(children.map(_.build), attributes.map(toLaminarMod(this)))
      conditionalShow.fold(childNode)(shouldDisplay =>
        L.div(L.child.maybe <-- shouldDisplay.map(Option.when(_)(childNode)))
      )

    override def addAttribute[R1](attribute: Attribute[R1]): Edge[R & R1] =
      this.copy(attributes = attribute :: attributes)

private[mira] val none: Val[None.type]             = Val(None)
private[mira] val always: Val[Boolean]             = Val(true)
private[mira] val noop: PartialFunction[Any, Unit] = _ => ()

private[mira] type LaminarElem = ReactiveHtmlElement[_ <: dom.html.Element]
private[mira] type LaminarMod  = Modifier[LaminarElem]
type KeyCode                   = Int
