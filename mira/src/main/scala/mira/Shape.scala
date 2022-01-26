package mira

import com.raquo.domtypes.generic.Modifier
import com.raquo.laminar.api.L.{Signal, Val}
import com.raquo.domtypes.generic.keys.Style as LaminarStyle
import com.raquo.laminar.nodes.ReactiveHtmlElement
import mira.Shape.*
import mira.projection.*
import org.scalajs.dom
import zio.{Runtime, UIO, ZIO}
import mira.*

import scala.language.postfixOps

trait Shape[-R](attributes: List[Attribute[R]]):

  import com.raquo.laminar.api.L

  def border     = BorderProjection(this)
  def color      = ColorProjection(this, L.color)
  def background = BackgroundProjection(this)
  def padding    = PaddingProjection(this)
  def margin     = MarginProjection(this)
  def height     = SizeProjection(this, L.height)
  def elevation  = ElevationProjection(this)
  def cursor     = CursorProjection(this)

  def hideWhen(condition: => Signal[Boolean]): Edge[R] = showWhen(condition.map(!_))
  def showWhen(condition: => Signal[Boolean]): Edge[R] = Edge(Seq(this), conditionalShow = Some(condition))

  def onClick[R1](zio: ZIO[R1, Nothing, Any]): Shape[R & R1] = addAttribute(Attribute.OnClick(zio))

  def onClick_(f: => Unit): Shape[R] = addAttribute(Attribute.OnClick(ZIO succeed f))

  def onKeyPress[R1](f: KeyCode => ZIO[R1, Nothing, Any]): Shape[R & R1] = addAttribute(Attribute.OnKeyPress(f))

  def onKeyPress[R1](f: PartialFunction[KeyCode, ZIO[R1, Nothing, Any]]): Shape[R & R1] =
    addAttribute(Attribute.OnKeyPress(f.orElse(noopZIO)))

  def onKeyPress_(f: KeyCode => Unit): Shape[R] = addAttribute(Attribute.OnKeyPress(ZIO succeed f(_)))

  def onKeyPress_(f: PartialFunction[KeyCode, Unit]): Shape[R] = onKeyPress(ZIO succeed f(_))

  def onMouse[R1](down: ZIO[R1, Nothing, Any], up: ZIO[R1, Nothing, Any]): Shape[R & R1] =
    addAttribute(Attribute.OnMouse(down, up))

  def onMouse_(down: => Unit, up: => Unit): Shape[R] = addAttribute(Attribute.OnMouse(ZIO succeed down, ZIO succeed up))

  def onHover[R1](in: ZIO[R1, Nothing, Any], out: ZIO[R1, Nothing, Any]): Shape[R & R1] =
    addAttribute(Attribute.OnHover(in, out))

  def onHover_(in: => Unit, out: => Unit): Shape[R] = addAttribute(Attribute.OnHover(ZIO succeed in, ZIO succeed out))

  /**
   * Create a new Shape containing this and that Shapes
   */
  def ++[R1](that: Shape[R1]): Edge[R & R1] = Edge(this :: that :: Nil)

  def build(toLaminarMod: (=> Shape[R]) => Attribute[R] => LaminarMod): LaminarElem

  def addAttribute[R1](attribute: Attribute[R1]): Shape[R & R1]

object Shape:

  val empty: Text[Any]                                        = Text("")
  def button: Button[Any]                                     = Button.empty
  val input: Input[Any]                                       = Input()
  def text(text: String): Text[Any]                           = Text.from(text)
  def text(text: AnyVal): Text[Any]                           = Text.from(text.toString)
  def text(textSignal: => Signal[String | AnyVal]): Text[Any] = Text.fromSignal(textSignal)
  def list[R](shape: Shape[R], shapes: Shape[R]*): Edge[R]    = Edge(shapes.prepended(shape))
  def list[R](shapes: => Signal[Iterable[Shape[R]]]): Edge[R] =
    Edge(Nil).addAttribute(Attribute.BindSignals(() => shapes))

  def when(condition: => Signal[Boolean]): When = When(condition)

  /**
   * Alias for [[Shape.text]]
   */
  def fromTextSignal(textSignal: => Signal[String | AnyVal]): Text[Any] = Shape.text(textSignal)

  /**
   * Alias for [[Shape.list]]
   */
  def fromShapesSignal[R](shapesSignal: => Signal[Iterable[Shape[R]]]): Edge[R] = Shape.list(shapesSignal)

private[mira] val none: Val[None.type]                                   = Val(None)
private[mira] val always: Val[Boolean]                                   = Val(true)
private[mira] val noop: PartialFunction[Any, Unit]                       = _ => ()
private[mira] val noopZIO: PartialFunction[Any, ZIO[Any, Nothing, Unit]] = _ => ZIO.unit

private[mira] type LaminarElem = ReactiveHtmlElement[_ <: dom.html.Element]
private[mira] type LaminarMod  = Modifier[LaminarElem]
type KeyCode                   = Int
