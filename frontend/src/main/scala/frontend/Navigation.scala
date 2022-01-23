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
      .background.color.primary
      .color.onPrimary
      .border.radius.small
      .border.none
      .padding.horizontal.large
      .margin.small
      .height.large
      .elevation.small
      .cursor.pointer
    ++
    Shape
      .button
      .text("CARGAR ORDEN")
      .onClick_(currentPage.set(Page.PlaceOrder))
      .background.color.secondary
      .color.onSecondary
      .border.radius.small
      .border.none
      .padding.horizontal.large
      .margin.small
      .height.large
      .elevation.small
      .cursor.pointer
