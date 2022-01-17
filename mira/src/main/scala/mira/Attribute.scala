package mira

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import zio.{Runtime, ZIO}

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

  case class OnClick(zio: ZIO[Any, Nothing, Any]) extends Attribute:
    def toLaminarModFor(elem: Shape)(using runtime: Runtime[Any]): LaminarMod =
      L.onClick --> { _ => runtime.unsafeRunAsync_(zio) }

  case class OnInput(f: String => Any) extends Attribute:
    def toLaminarModFor(elem: Shape)(using runtime: Runtime[Any]): LaminarMod = L.onInput.mapToValue --> { f(_) }

  case class OnKeyPress(f: Int => Any) extends Attribute:
    def toLaminarModFor(elem: Shape)(using runtime: Runtime[Any]): LaminarMod = L.onKeyPress.map(_.keyCode) --> { f(_) }

  case class Placeholder(text: String) extends Attribute:
    def toLaminarModFor(elem: Shape)(using runtime: Runtime[Any]): LaminarMod = elem match {
      case Shape.Node(_, _, "input") => L.placeholder := text
      case _                         => L.emptyMod // TODO should not happen
    }
