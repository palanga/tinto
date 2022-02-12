package frontend

import mira.*
import zio.{Runtime, ZEnv}

/**
 * TODO implementar inject (actualizar a zio 2) en Shape
 */
object Main:

  implicit val runtime: Runtime[ZEnv] = Runtime.default

  val addArticlePage = ArticleForm.view ++ Catalog.view
  val placeOrderPage = order.form.OrderForm.view

  val root =
    Shape.column(
      Navigation.view,
      addArticlePage
        .showWhen(Navigation.currentPage.signal.map(_ == Navigation.Page.Catalog))
        .margin.small
        .padding.small
        .elevation.low,
      placeOrderPage
        .showWhen(Navigation.currentPage.signal.map(_ == Navigation.Page.PlaceOrder))
        .margin.small
        .padding.small,
    )

  def main(args: Array[String]): Unit =
    import com.raquo.laminar.api.L.*
    runtime unsafeRunAsync Catalog.loadCatalog()
    renderOnDomContentLoaded(
      org.scalajs.dom.document.querySelector("#app"),
      root.build(toLaminarModDefault),
    )
