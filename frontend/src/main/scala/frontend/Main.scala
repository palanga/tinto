package frontend

import core.{LocalStoreManager, StoreManager}
import mira.*
import zio.*

/**
 * TODO implementar inject (actualizar a zio 2) en Shape
 */
object Main:

  val defaultRuntime: Runtime[ZEnv] = Runtime.default

  val deps: ZLayer[Any, Nothing, StoreManager] =
    zio.ZIO.service[StoreManager]
      .provide(
        LocalStoreManager.build.toLayer
      )
      .toLayer

  implicit val runtime: Runtime[StoreManager] =
    defaultRuntime unsafeRun deps.toRuntime(zio.RuntimeConfig.default).useNow

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
    runtime unsafeRunAsync Catalog.loadCatalog().orDie
    renderOnDomContentLoaded(
      org.scalajs.dom.document.querySelector("#app"),
      root.build(toLaminarModDefault),
    )
