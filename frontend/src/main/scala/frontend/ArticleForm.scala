package frontend

import client.scalajs.client.fetch
import com.raquo.airstream.state.Var
import core.*
import mira.Shape
import zio.ZIO

import java.util.UUID

object ArticleForm:

  private val title    = Var("")
  private val subtitle = Var("")
  private val price    = Var("")

  val view =
    Shape.list(
      Shape.input.text(title.signal).onInput_(title.set).placeholder("Radix"),
      Shape.input.text(subtitle.signal).onInput_(subtitle.set).placeholder("malbec"),
      Shape.input.text(price.signal).onInput_(price.set).placeholder("800"),
      Shape.text("AGREGAR").onClick(addArticle().map(Catalog.addArticle).orDie),
    )

  private def addArticle() =
    ZIO
      .fromEither(for {
        title <- NonEmptyString(title.now())
        price <- ARS.fromString("ARS " + price.now()).toRight(new IllegalArgumentException(price.now()))
      } yield AddArticleForm(title, subtitle.now(), price))
      .flatMap(fetch(web.api.catalog.add))
