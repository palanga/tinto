package mira

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import zio.{Runtime, ZIO}

//enum Option[-T]:
//
//  def toLala(elT: T) = ???
//
//  case Some(x: Any)
//  case None

sealed trait Attribute[-R]:
  def toLaminarModFor(elem: Shape[_])(using runtime: Runtime[R]): LaminarMod

object Attribute:

  case class Placeholder(text: String) extends Attribute[Any]:
    def toLaminarModFor(elem: Shape[_])(using runtime: Runtime[Any]): LaminarMod = elem match {
      case Shape.Node(_, _, "input") => L.placeholder := text
      case _                         => L.emptyMod // TODO should not happen
    }

  case class BindSignal(signal: Signal[Any]) extends Attribute[Any]: // TODO signal de any val o string
    def toLaminarModFor(elem: Shape[_])(using runtime: Runtime[Any]): LaminarMod = elem match {
      case Shape.Node(_, _, "input") => L.value <-- signal.map(_.toString)
      case _                         => L.child.text <-- signal.map(_.toString) // TODO ??? deah
    }

  case class BindSignals(signal: Signal[Seq[Shape[Any]]]) extends Attribute[Any]:
    def toLaminarModFor(elem: Shape[_])(using runtime: Runtime[Any]): LaminarMod =
      L.children <-- signal.map(_.map(_.build))

  case class OnClick[R](zio: ZIO[R, Nothing, Any]) extends Attribute[R]:
    def toLaminarModFor(elem: Shape[_])(using runtime: Runtime[R]): LaminarMod =
      L.onClick --> { _ => runtime.unsafeRunAsync_(zio) }

  case class OnInput(f: String => Any) extends Attribute[Any]:
    def toLaminarModFor(elem: Shape[_])(using runtime: Runtime[Any]): LaminarMod = L.onInput.mapToValue --> { f(_) }

  case class OnKeyPress(f: Int => Any) extends Attribute[Any]:
    def toLaminarModFor(elem: Shape[_])(using runtime: Runtime[Any]): LaminarMod =
      L.onKeyPress.map(_.keyCode) --> { f(_) }
