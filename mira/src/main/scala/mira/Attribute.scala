package mira

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.*
import com.raquo.laminar.api.L.*
import mira.Attribute.*
import org.scalajs.dom
import zio.{Runtime, ZIO}

enum Attribute[-R]:
  case Placeholder(text: String)
  case BindSignal(signal: () => Signal[String])
  case BindSignals(signal: () => Signal[Iterable[Shape[R]]])
  case OnClick(zio: ZIO[R, Nothing, Any])
  case OnInput(f: String => ZIO[R, Nothing, Any])
  case OnKeyPress(f: Int => ZIO[R, Nothing, Any])
  case OnHover(in: ZIO[R, Nothing, Any], out: ZIO[R, Nothing, Any])
  case OnMouse(down: ZIO[R, Nothing, Any], up: ZIO[R, Nothing, Any])
  case Style(mod: () => LaminarMod)

class StyleType(val toLaminarMod: () => LaminarMod)

def toLaminarModDefault[R](shape: => Shape[R])(attribute: Attribute[R])(using runtime: Runtime[R]): LaminarMod =
  def run = runtime.unsafeRunAsync
  attribute match {
    case Placeholder(text)   => if isInput(shape) then L.placeholder := text else L.emptyMod
    case BindSignal(signal)  => if isInput(shape) then L.value <-- signal() else L.child.text <-- signal()
    case BindSignals(signal) => L.children <-- signal().map(_.map(shape => shape.build(toLaminarModDefault)).toSeq)
    case OnClick(zio)        => L.onClick --> { _ => run(zio) }
    case OnInput(f)          => L.onInput.mapToValue --> { runtime unsafeRunAsync f(_) }
    case OnKeyPress(f)       => L.onKeyPress.map(_.keyCode) --> { runtime unsafeRunAsync f(_) }
    case OnHover(in, out)    => List(onMouseOver --> { _ => run(in) }, onMouseOut --> { _ => run(out) })
    case OnMouse(down, up)   => List(L.onMouseDown --> { _ => run(down) }, L.onMouseUp --> { _ => run(up) })
    case Style(mod)          => mod()
  }

private def isInput(shape: => Shape[_]) = shape match {
  case _: mira.Input[_] => true
  case _                => false
}

val onMouseOver = new EventProp[dom.MouseEvent]("mouseover")
val onMouseOut  = new EventProp[dom.MouseEvent]("mouseout")
