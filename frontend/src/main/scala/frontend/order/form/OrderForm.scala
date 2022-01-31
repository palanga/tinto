package frontend.order.form

import com.raquo.airstream.state.Var
import core.*
import frontend.Catalog
import mira.Shape
import zio.console.putStrLn

import java.util.UUID

object OrderForm:

  val currentStep = Var(Step.SelectArticles)

  enum Step:
    def next = this match {
      case Step.ReviewOrder => this
      case _                => Step.fromOrdinal(this.ordinal + 1)
    }
    def prev = this match {
      case Step.SelectArticles => this
      case _                   => Step.fromOrdinal(this.ordinal - 1)
    }
    case SelectArticles, CustomerInfo, ReviewOrder

  val prevStep =
    Shape
      .button.textOnly
      .text("ATRAS")
      .onClick_(currentStep.update(_.prev))
      .hideWhen(currentStep.signal.map(_ == Step.SelectArticles))

  val nextStep =
    Shape
      .button.contained
      .text("SIGUIENTE")
      .onClick_(currentStep.update(_.next))
      .hideWhen(currentStep.signal.map(_ == Step.ReviewOrder))

  val send =
    Shape
      .button.contained
      .text("CONFIRMAR")
      .onClick(placeOrder())
      .showWhen(currentStep.signal.map(_ == Step.ReviewOrder))

  val view =
    Shape.list(prevStep, nextStep, send)
      ++ Shape.list(
        SelectArticles.view.showWhen(currentStep.signal.map(_ == Step.SelectArticles)),
        CustomerInfo.view.showWhen(currentStep.signal.map(_ == Step.CustomerInfo)),
        ReviewOrder.view.showWhen(currentStep.signal.map(_ == Step.ReviewOrder)),
      )

  private def placeOrder() =
//    for {
//      items <- SelectArticles.countById.now()
//    } yield ???
//    client.scalajs.client.fetch(web.api.orders.place)(PlaceOrderForm(???, ???))
    putStrLn("jaja").ignore

  object SelectArticles:

    val countById: Var[Map[UUID, Int]] = Var(Map.empty)

    val view = Shape.list(Catalog.catalog.signal.map(_.map(renderArticle)))

    private def renderArticle(id: UUID, article: Article) =
      article match {
        case Article(title, subtitle, price) =>
          Shape.list(
            Shape.text(title.self),
            Shape.text(subtitle),
            Shape.text(price.toString),
            Shape.text("+").onClick_(countById.update(_.updatedWith(id)(incrementOrCreate))),
            Shape.text(countById.signal.map(_.getOrElse(id, 0))),
            Shape.text("-").onClick_(countById.update(_.updatedWith(id)(decrementOrRemove))),
          )
      }

    def incrementOrCreate(maybeInt: Option[Int]): Option[Int] =
      maybeInt.map(_ + 1).fold(Some(1))(Some(_))
    def decrementOrRemove(maybeInt: Option[Int]): Option[Int] =
      maybeInt.map(_ - 1).fold(None)(v => if v == 0 then None else Some(v))

  object CustomerInfo:
    val view = Shape.text("en progreso")

  object ReviewOrder:
    val view = Shape.text("en progreso")
