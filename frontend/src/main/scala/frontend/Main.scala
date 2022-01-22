package frontend

import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L.*
import mira.*
import zio.console.putStrLn
import zio.{Runtime, ZEnv}
import client.scalajs.client.fetch
import endpoints.*

object Main:

  implicit val runtime: Runtime[ZEnv] = Runtime.default

  val addArticlePage = ArticleForm.view ++ Catalog.view
  val placeOrderPage = order.form.OrderForm.view

  val root =
    Navigation.view
      ++ addArticlePage.showWhen(Navigation.currentPage.signal.map(_ == Navigation.Page.Catalog))
      ++ placeOrderPage.showWhen(Navigation.currentPage.signal.map(_ == Navigation.Page.PlaceOrder))

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
