package frontend

import com.raquo.laminar.api.L.*
import mira.*
import zio.{Runtime, ZEnv}

object Main:

  implicit val runtime: Runtime[ZEnv] = Runtime.default

  val addArticlePage = ArticleForm.view ++ Catalog.view
  val placeOrderPage = order.form.OrderForm.view

  val root =
    Shape.list(
      Navigation.view,
      addArticlePage
        .showWhen(Navigation.currentPage.signal.map(_ == Navigation.Page.Catalog))
        .margin.small
        .padding.small
        .elevation.low,
      placeOrderPage
        .showWhen(Navigation.currentPage.signal.map(_ == Navigation.Page.PlaceOrder))
        .margin.small
        .padding.small
        .elevation.low,
    )

  import com.raquo.laminar.api.L

  def main(args: Array[String]): Unit =
    runtime unsafeRunAsync_ Catalog.loadCatalog()
    renderOnDomContentLoaded(
      org.scalajs.dom.document.querySelector("#app"),
      div(
        root.build
//        fetchNoZIO.build,
//        example.root.build,
      ),
    )
