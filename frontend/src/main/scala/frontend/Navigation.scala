package frontend

import com.raquo.airstream.state.Var
import mira.{Button, Edge, Shape}

object Navigation:

  enum Page:
    case Catalog, PlaceOrder

  val currentPage = Var(Page.Catalog)

  val view =
    Shape.list(
      Shape
        .button
        .textOnly
        .text("CATALOGO")
        .onClick_(currentPage.set(Page.Catalog))
//        .color.onPrimary
      ,
      Shape
        .button
        .contained
        .text("CARGAR ORDEN")
        .onClick_(currentPage.set(Page.PlaceOrder)),
      Shape
        .button
        .outlined
        .text("CARGAR ORDEN")
        .onClick_(currentPage.set(Page.PlaceOrder)),
//            .color.onPrimary
    )
//      .background.color.transparent
