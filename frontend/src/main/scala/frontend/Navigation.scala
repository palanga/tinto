package frontend

import com.raquo.airstream.state.Var
import mira.{Button, Edge, Shape}

object Navigation:

  enum Page:
    case Catalog, PlaceOrder

  val currentPage = Var(Page.Catalog)

  val view =
    Shape.row(
      Shape
        .button
        .textOnly
        .text("CATALOGO")
        .onClick_(currentPage.set(Page.Catalog))
        .color.onPrimary,
      Shape
        .button
        .textOnly
        .text("CARGAR ORDEN")
        .onClick_(currentPage.set(Page.PlaceOrder))
        .color.onPrimary,
    )
      .background.color.primary
      .elevation.medium
