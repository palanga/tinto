package mira.projection

import com.raquo.laminar.api.*
import mira.*

class BackgroundProjection[-R](private val self: Shape[R]):
  def color = ColorProjection(self, L.backgroundColor)
