package frontend

import com.raquo.airstream.state.Var
import core.{Article, Customer, Ident, Item, Nat, PlaceOrderForm}

case class State(
  articles: Var[List[Ident[Article]]],
  order: OrderState,
):

  def loadArticles(articles: List[Ident[Article]]) = dispatch("load articles")(this.articles.set(articles))

  private def dispatch = dispatchWrapper(this)

  override def toString: String =
    s"""|state:
        |  articles: ${articles.now().mkString("[", ", ", "]")}
        |  ${order.toString}""".stripMargin

case class OrderState(items: Var[List[Item]], customer: Var[Customer]):

  def increment(article: Ident[Article]) = dispatch(s"increment $article") {
    items.update(items =>
      items.map(item => if item.article.id == article.id then item.copy(amount = item.amount + Nat.ONE) else item)
    )
  }

  def decrement(article: Ident[Article]) = dispatch(s"decrement $article") {

    items.update { items =>
      items
        .find(_.article.id == article.id)
        .map { item =>
          if item.amount == Nat.ONE then items.filter(_.article.id == article.id)
          else {
            items.map(item =>
              if item.article.id == article.id then item.copy(amount = Nat(item.amount - 1).getOrElse(Nat.ONE))
              else item
            ) // TODO flatmap to option
          }

        }
        .getOrElse(items)
    }
  }

  private def dispatch = dispatchWrapper(this)

  override def toString: String =
    s"""|order state:
        |  items: ${items.now().mkString("[", ", ", "]")}
        |  customer: ${customer.now().toString}""".stripMargin
