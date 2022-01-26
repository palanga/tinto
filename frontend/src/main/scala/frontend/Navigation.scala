package frontend

import com.raquo.airstream.state.Var
import mira.Shape

object Navigation:

  enum Page:
    case Catalog, PlaceOrder

  val currentPage = Var(Page.Catalog)

  val view =
    Shape
      .button
      .text("CATALOGO")
      .onClick_(currentPage.set(Page.Catalog))
    ++
    Shape
      .button
      .text("CARGAR ORDEN")
      .onClick_(currentPage.set(Page.PlaceOrder))
