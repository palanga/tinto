package frontend

import client.scalajs.client.fetch
import com.raquo.airstream.state.Var
import core.*
import mira.Shape
import zio.ZIO

import java.util.UUID

object Catalog:

  val catalog: Var[Map[UUID, Article]] = Var(Map.empty)

  val view = Shape.column(catalog.signal.map(_.values.map(renderArticle)))

  def addArticle(article: Ident[Article]): Unit = catalog.update(_ + (article.id -> article.self))

  def loadCatalog() = fetch(web.api.catalog.all)(()).map(_.groupMapReduce(_.id)(_.self)((a, _) => a)).map(catalog.set)

  private def renderArticle(article: Article) = article match {
    case Article(title, subtitle, price) =>
      Shape.row(
        Shape.text(title.self),
        Shape.text(subtitle),
        Shape.text(price.toString),
      )
  }
