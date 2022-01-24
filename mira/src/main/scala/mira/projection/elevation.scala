package mira.projection

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.*
import mira.*
import mira.projection.Elevation.*
import zio.ZIO

class ElevationProjection[-R](private val self: Shape[R]):
  def none    = self.addAttribute(Attribute.Style(() => L.boxShadow := shadow(None)))
  def lowest  = self.addAttribute(Attribute.Style(() => L.boxShadow := shadow(Lowest)))
  def low     = self.addAttribute(Attribute.Style(() => L.boxShadow := shadow(Low)))
  def medium  = self.addAttribute(Attribute.Style(() => L.boxShadow := shadow(Medium)))
  def high    = self.addAttribute(Attribute.Style(() => L.boxShadow := shadow(High)))
  def highest = self.addAttribute(Attribute.Style(() => L.boxShadow := shadow(Highest)))

  def dynamic(elevation: Signal[Elevation]) =
    self.addAttribute(Attribute.Style(() => L.boxShadow <-- elevation.map(shadow)))

enum Elevation:
  case None, Lowest, Low, Medium, High, Highest

private def shadow(elevation: Elevation) = elevation match {
  case None    => "gray 1px 1px 0px -2px"
  case Lowest  => "gray 1px 1px 4px -2px"
  case Low     => "gray 1px 1px 6px -2px"
  case Medium  => "gray 1px 1px 8px -2px"
  case High    => "gray 1px 1px 10px -2px"
  case Highest => "gray 1px 1px 12px -2px"
}
