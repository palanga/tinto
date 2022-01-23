package mira.projection

import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.*
import mira.*
import zio.ZIO

class ElevationProjection[-R](private val self: Shape[R]):
  private val isHovered                          = Var(false)
  private val isPressed                          = Var(false)
  private val zipped: Signal[(Boolean, Boolean)] = isHovered.signal combineWith isPressed.signal

  private def withStatus =
    self
      .addAttribute(Attribute.OnHover(ZIO succeed isHovered.set(true), ZIO succeed isHovered.set(false)))
      .addAttribute(Attribute.OnMouse(ZIO succeed isPressed.set(true), ZIO succeed isPressed.set(false)))
      .addAttribute(Attribute.Style(() => L.opacity <-- zipped.map(opacity)))

  def none     = withStatus.addAttribute(Attribute.Style(() => L.boxShadow <-- zipped.map(shadowZ(0))))
  def smallest = withStatus.addAttribute(Attribute.Style(() => L.boxShadow <-- zipped.map(shadowZ(2))))
  def small    = withStatus.addAttribute(Attribute.Style(() => L.boxShadow <-- zipped.map(shadowZ(4))))
  def medium   = withStatus.addAttribute(Attribute.Style(() => L.boxShadow <-- zipped.map(shadowZ(6))))
  def large    = withStatus.addAttribute(Attribute.Style(() => L.boxShadow <-- zipped.map(shadowZ(8))))
  def largest  = withStatus.addAttribute(Attribute.Style(() => L.boxShadow <-- zipped.map(shadowZ(10))))

  def dynamic(elevation: Signal[Int]) = self.addAttribute(Attribute.Style(() => L.boxShadow <-- elevation.map(shadow)))

private def shadow(elevation: Int) = s"0px 3px ${elevation}px -4px black"

private def shadowZ(initialElevation: Int)(status: (Boolean, Boolean)) =
  val elevation = status match {
    case (false, false) => initialElevation
    case (true, false)  => initialElevation + 2
    case _              => 0
  }
  s"0px 3px ${elevation}px -4px black"

private def opacity(status: (Boolean, Boolean)) = status match {
  case (true, false) => "90%"
  case _             => "100%"
}
