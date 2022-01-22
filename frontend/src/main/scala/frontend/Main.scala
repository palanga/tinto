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

  val a =
    L.div(
      L.child.maybe <-- Navigation.currentPage.signal
        .map(_ == Navigation.Page.Catalog)
        .map(Option.when(_)(renderCatalog())),
      L.child.maybe <-- Navigation.currentPage.signal
        .map(_ == Navigation.Page.PlaceOrder)
        .map(Option.when(_)(renderCatalog2())),
    )

  def renderCatalog() =
    L.div(
      L.children <-- Catalog.catalog.signal.map(_.values.map(renderArticle).toSeq)
    )

  def renderArticle(article: core.Article) =
    L.div(
      article.title.self
    )

  def renderCatalog2() =
    L.div(
      L.children <-- Catalog.catalog.signal.map(_.values.map(renderArticle2).toSeq)
    )

  def renderArticle2(article: core.Article) =
    L.div(
      article.title.self,
      article.price.toString,
    )

  def main(args: Array[String]): Unit =
    runtime unsafeRunAsync_ Catalog.loadCatalog()
    renderOnDomContentLoaded(
      org.scalajs.dom.document.querySelector("#app"),
      div(
        root.build,
        a,
//        fetchNoZIO.build,
//        example.root.build,
      ),
    )
