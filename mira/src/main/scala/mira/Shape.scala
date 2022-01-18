package mira

import com.raquo.domtypes.generic.Modifier
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import zio.{Runtime, UIO, ZIO}
import Shape.*

import scala.language.postfixOps

sealed trait Shape[-R](attributes: List[Attribute[R]]):

  def showWhen(condition: Signal[Boolean]): Edge[R]
  def hideWhen(condition: Signal[Boolean]): Edge[R] = showWhen(condition.map(!_))

  /**
   * Create a new Shape containing this and that Shapes
   */
  def ++[R1](that: Shape[R1]): Edge[R & R1] = Edge(this :: that :: Nil)

  def build(using runtime: Runtime[R]): LaminarElem

  protected def addAttribute[R1](attribute: Attribute[R1]): Shape[R & R1]

object Shape:

  val empty: Node[Any]                                     = Node("")
  val input: Node[Any]                                     = Node("", kind = "input")
  def text(text: String): Node[Any]                        = Node(text)
  def text(text: AnyVal): Node[Any]                        = Node(text.toString)
  def text(textSignal: Signal[String | AnyVal]): Node[Any] = empty.bind(textSignal)
  def list[R](shapes: Signal[List[Shape[R]]]): Node[R]     = empty.bindAll(shapes)
  def list[R](shape: Shape[R], shapes: Shape[R]*): Edge[R] = Edge(shapes.prepended(shape))

  def when(condition: Signal[Boolean]): When = When(condition)

  /**
   * Alias for [[Shape.text]]
   */
  def fromTextSignal(textSignal: Signal[String | AnyVal]): Node[Any] = Shape.text(textSignal)

  /**
   * Alias for [[Shape.list]]
   */
  def fromShapesSignal[R](shapesSignal: Signal[List[Shape[R]]]): Node[R] = Shape.list(shapesSignal)

  case class Node[-R](
    private val text: String,
    private val attributes: List[Attribute[R]] = Nil,
    private val kind: "div" | "input" = "div",
  ) extends Shape(attributes):

    def bind(signal: Signal[String | AnyVal]): Node[R] = this.addAttribute(Attribute.BindSignal(signal.map(_.toString)))

    def bindAll[R1](signal: Signal[Seq[Shape[R1]]]): Node[R & R1] = this.addAttribute(Attribute.BindSignals(signal))

    def placeholder(text: String): Node[R] = this.addAttribute(Attribute.Placeholder(text))

    def onClick[R1](zio: ZIO[R1, Nothing, Any]): Node[R & R1] = this.addAttribute(Attribute.OnClick(zio))

    def onClick_(f: => Unit): Node[R] = addAttribute(Attribute.OnClick(ZIO succeed f))

    def onInput_(f: String => Unit): Node[R] = addAttribute(Attribute.OnInput(ZIO succeed f(_))).copy(kind = "input")

    def onInput[R1](zio: String => ZIO[R1, Nothing, Any]): Node[R & R1] =
      addAttribute(Attribute.OnInput(zio)).copy(kind = "input")

    def onKeyPress[R1](f: KeyCode => ZIO[R1, Nothing, Any]): Node[R & R1] = addAttribute(Attribute.OnKeyPress(f))

    def onKeyPress[R1](f: PartialFunction[KeyCode, ZIO[R1, Nothing, Any]]): Node[R & R1] =
      addAttribute(Attribute.OnKeyPress(f.orElse(noopZIO)))

    def onKeyPress_(f: KeyCode => Unit): Node[R] = addAttribute(Attribute.OnKeyPress(ZIO succeed f(_)))

    def onKeyPress_(f: PartialFunction[KeyCode, Unit]): Node[R] =
//      f.
//      val a: KeyCode => UIO[Unit] = ZIO succeed f(_)
      onKeyPress(ZIO succeed f(_))

    override def showWhen(condition: Signal[Boolean]): Edge[R] = Edge(Seq(this), conditionalShow = Some(condition))

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

    def onClick[R1](zio: ZIO[R1, Nothing, Any]): Edge[R & R1] = this.addAttribute(Attribute.OnClick(zio))

    def onClick(f: => Unit): Edge[R] = this.addAttribute(Attribute.OnClick(ZIO succeed f))

    /**
     * Alias for [[Edge.prependChild]]
     */
    def +:[R1](node: Node[R1]): Edge[R & R1] = this.prependChild(node)

    /**
     * Alias for [[Edge.prependChild]]
     */
    def ::[R1](node: Node[R1]): Edge[R & R1] = this.prependChild(node)

    override def showWhen(condition: Signal[Boolean]): Edge[R] = this.copy(conditionalShow = Some(condition))

    override def build(using runtime: Runtime[R]): LaminarElem =
      val childNode = L.div(children.map(_.build), attributes.map(toLaminarMod(this)))
      conditionalShow.fold(childNode)(shouldDisplay =>
        L.div(L.child.maybe <-- shouldDisplay.map(Option.when(_)(childNode)))
      )

    override def addAttribute[R1](attribute: Attribute[R1]): Edge[R & R1] =
      this.copy(attributes = attribute :: attributes)

    /**
     * Add a child node at the start of this Shape
     */
    private def prependChild[R1](node: Node[R1]): Edge[R & R1] = this.copy(this.children.prepended(node))

private[mira] val none: Val[None.type]                                   = Val(None)
private[mira] val always: Val[Boolean]                                   = Val(true)
private[mira] val noop: PartialFunction[Any, Unit]                       = _ => ()
private[mira] val noopZIO: PartialFunction[Any, ZIO[Any, Nothing, Unit]] = _ => ZIO.unit

private[mira] type LaminarElem = ReactiveHtmlElement[_ <: dom.html.Element]
private[mira] type LaminarMod  = Modifier[LaminarElem]
type KeyCode                   = Int

class When(condition: Signal[Boolean]):
  def show[R](shape: Shape[R]): Edge[R] = shape.showWhen(condition)
  def hide[R](shape: Shape[R]): Edge[R] = shape.hideWhen(condition)
