package mira

import mira.Shape.*
import mira.projection.ElevationProjection
import zio.*
import com.raquo.laminar.api.L.{Var, Signal}
import com.raquo.laminar.api.L
import mira.projection.Elevation
import mira.projection.shadow

class Button[-R](
  private[mira] val text: String,
  private[mira] val attributes: List[Attribute[R]] = Nil,
  private[mira] val state: Var[ButtonState] = Var(ButtonState()),
  private[mira] val defaultElevation: Elevation = Elevation.None,
) extends Shape(attributes):

//  val currentElevation: Signal[Elevation] = defaultElevation.signal.map(calculateElevation(_, this.state))

  def currentElevation = state.signal.map(calculateElevation(defaultElevation, _))

  def text(text: String): Button[R] = new Button(text, attributes, state, defaultElevation)

  override def elevation: ElevationProjection[R] = ButtonElevationProjection(this)

  override def build(toLaminarMod: (=> Shape[R]) => Attribute[R] => LaminarMod): LaminarElem =
    import com.raquo.laminar.api.*

    def additionalAttributes = List(
      Attribute.Style(() => L.boxShadow <-- currentElevation.map(shadow)),
      Attribute.OnMouse(ZIO succeed state.update(_.press), ZIO succeed state.update(_.unPress)),
      Attribute.OnHover(ZIO succeed state.update(_.hover), ZIO succeed state.update(_.unHover)),
    )

    def laminarMods = (attributes ++ additionalAttributes).map(toLaminarMod(this))
    L.button(text, laminarMods)

  override def addAttribute[R1](attribute: Attribute[R1]): Button[R & R1] =
    new Button(text, attribute :: attributes, state, defaultElevation)

  def copy[R1 <: R](
    text: String = text,
    attributes: List[Attribute[R1]] = attributes,
    state: Var[ButtonState] = state,
    defaultElevation: Elevation = defaultElevation,
  ) = new Button[R1](text, attributes, state, defaultElevation)

private def calculateElevation(default: Elevation, state: ButtonState) = state match {
  case ButtonState(_, true)     => Elevation.None
  case ButtonState(true, false) => Elevation.fromOrdinal(default.ordinal + 1)
  case _                        => default
}

object Button:

  def apply(): Button[Any] =
    new Button("")
      .border.radius.small
      .border.none
      .padding.horizontal.large
      .margin.small
      .height.large
      .elevation.low
      .cursor.pointer
      .background.color.primary
      .color.onPrimary
      .asInstanceOf[Button[Any]]

  def empty: Button[Any] = Button()

private case class ButtonState(isHovered: Boolean = false, isPressed: Boolean = false):
  def hover   = copy(isHovered = true)
  def unHover = copy(isHovered = false)
  def press   = copy(isPressed = true)
  def unPress = copy(isPressed = false)

//object ButtonState:
//  def unapply(self: ButtonState) = self.isPressed -> self.isHovered

class ButtonElevationProjection[-R](private val self: Button[R]) extends ElevationProjection[R](self):

  override def none    = self.copy(defaultElevation = Elevation.None)
  override def lowest  = self.copy(defaultElevation = Elevation.Lowest)
  override def low     = self.copy(defaultElevation = Elevation.Low)
  override def medium  = self.copy(defaultElevation = Elevation.Medium)
  override def high    = self.copy(defaultElevation = Elevation.High)
  override def highest = self.copy(defaultElevation = Elevation.Highest)

  override def dynamic(elevation: Signal[Elevation]) =
//    import com.raquo.laminar.api.*
//    elevation --> self.defaultElevation
    self
