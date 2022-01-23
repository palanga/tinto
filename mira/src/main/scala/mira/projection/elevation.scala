package mira.projection

import com.raquo.airstream.state.Var
import com.raquo.domtypes.generic.keys.Style as LaminarStyle
import com.raquo.laminar.api.*
import mira.*
import zio.ZIO

class ElevationProjection[-R](private val self: Shape[R]):
  private val isHovered = Var(false)

  def withHover =
    self.addAttribute(Attribute.OnHover(ZIO.succeed(isHovered.set(true)), ZIO succeed isHovered.set(false)))

  def none     = withHover.addAttribute(Attribute.Style(() => L.boxShadow <-- isHovered.signal.map(shadow(0))))
  def smallest = withHover.addAttribute(Attribute.Style(() => L.boxShadow <-- isHovered.signal.map(shadow(2))))
  def small    = withHover.addAttribute(Attribute.Style(() => L.boxShadow <-- isHovered.signal.map(shadow(4))))
  def medium   = withHover.addAttribute(Attribute.Style(() => L.boxShadow <-- isHovered.signal.map(shadow(6))))
  def large    = withHover.addAttribute(Attribute.Style(() => L.boxShadow <-- isHovered.signal.map(shadow(8))))
  def largest  = withHover.addAttribute(Attribute.Style(() => L.boxShadow <-- isHovered.signal.map(shadow(10))))

private def shadow(initialElevation: Int)(isHovered: Boolean) =
  val elevation = if isHovered then initialElevation + 2 else initialElevation
  s"0px 3px ${elevation}px -4px black"
