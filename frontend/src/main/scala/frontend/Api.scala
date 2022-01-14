package frontend

import com.raquo.airstream.web.AjaxEventStream
//import com.raquo.laminar.api.Laminar.EventStream
import core.{ARS, Article, Ident, NonEmptyString}
import org.scalajs.dom.console

object articles_api:

  import codecs.*
  import zio.json.*

  def all =
    com.raquo.airstream.core.EventStream
      .fromValue("""{"data": {"articles": [{"id": "an id", "self": null}]}}""")
      .map(_.fromJson[MyResponse[ArticleIds]])
      .map(_.left.map(e => MyResponse(ArticleIds(Nil), Some(e))).merge)

//    AjaxEventStream
//      .post("http://localhost:8088/api/graphql", data = s"""{"query": "$allArticles"}""")
//      .map(_.responseText)
//    .map(decode[Response])
//    .map(_.left.map(e => Response(Articles(Nil), Some(e.getMessage))).merge)
//    .map(console.log(_))
//    .map(Article)

case class MyResponse[T](data: T, errors: Option[String] = None, messages: Option[String] = None)

case class ArticleIds(articles: List[Id])

case class Id(id: String)

case class Articles(articles: List[Ident[Article]])

val allArticles = ??? //api.queries.allArticles.replaceAll("\n", "")

object codecs:

  import zio.json.*

  implicit val nesDecoder: JsonDecoder[NonEmptyString] =
    zio.json.JsonDecoder.string.mapOrFail(NonEmptyString(_).left.map(_.getMessage))

  implicit val arsDecoder: JsonDecoder[ARS.Price] =
    zio.json.JsonDecoder.string
      .mapOrFail(input => ARS.fromString(input).toRight(s"price decode failure <<$input>>"))

  implicit val articleDecoder: JsonDecoder[Article]       = DeriveJsonDecoder.gen
  implicit val identDecoder: JsonDecoder[Ident[Article]]  = DeriveJsonDecoder.gen
  implicit val idDecoder: JsonDecoder[Id]                 = DeriveJsonDecoder.gen
  implicit val articleIdsDecoder: JsonDecoder[ArticleIds] = DeriveJsonDecoder.gen

  implicit val articlesDecoder: JsonDecoder[Articles]                = DeriveJsonDecoder.gen
  implicit val responseDecoder: JsonDecoder[MyResponse[Articles]]    = DeriveJsonDecoder.gen
  implicit val response2Decoder: JsonDecoder[MyResponse[ArticleIds]] = DeriveJsonDecoder.gen
