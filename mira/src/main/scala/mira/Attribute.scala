package mira

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import mira.Attribute.*
import zio.{Runtime, ZIO}

enum Attribute[-R]:
  case Placeholder(text: String)
  case BindSignal(signal: Signal[String])
  case BindSignals(signal: Signal[Seq[Shape[R]]])
  case OnClick(zio: ZIO[R, Nothing, Any])
  case OnInput(f: String => Any)
  case OnKeyPress(f: Int => Any)

def toLaminarMod[R](shape: Shape[_])(attribute: Attribute[R])(using runtime: Runtime[R]): LaminarMod = attribute match {
  case Placeholder(text)   => if isInput(shape) then L.placeholder := text else L.emptyMod
  case BindSignal(signal)  => if isInput(shape) then L.value <-- signal else L.child.text <-- signal
  case BindSignals(signal) => L.children <-- signal.map(_.map(_.build))
  case OnClick(zio)        => L.onClick --> { _ => runtime.unsafeRunAsync_(zio) }
  case OnInput(f)          => L.onInput.mapToValue --> { f(_) }
  case OnKeyPress(f)       => L.onKeyPress.map(_.keyCode) --> { f(_) }
}

private def isInput(shape: Shape[_]) = shape match {
  case Shape.Node(_, _, "input") => true
  case _                         => false
}
