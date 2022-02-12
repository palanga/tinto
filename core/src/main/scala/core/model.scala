package core

import aconcagua.price.mono.{Currency, Prices}
import zio.*
import zio.Random.nextUUID

import java.util.UUID
import scala.collection.immutable

val ARS: Prices["ARS"] = Prices.ofCurrency(Currency.ARS)

case class Article(
  title: NonEmptyString,
  subtitle: String,
  price: ARS.Price,
)

case class Customer(
  name: Name,
  contactInfo: ContactInfo,
  address: Address,
)

case class Order private (
  items: NonEmptySet[Item],
  customer: Customer,
  status: Status = Status.Open,
):

  def pay: Either[Error, Order] = status match {
    case Status.Open      => Right(this.copy(status = Status.Paid))
    case Status.Delivered => Right(this.copy(status = Status.Closed))
    case next             => Left(Error.IllegalTransition(status, next))
  }

  def deliver: Either[Error, Order] = status match {
    case Status.Open => Right(this.copy(status = Status.Delivered))
    case Status.Paid => Right(this.copy(status = Status.Closed))
    case next        => Left(Error.IllegalTransition(status, next))
  }

  def cancel: Either[Error, Order] = status match {
    case Status.Open => Right(this.copy(status = Status.Cancelled))
    case next        => Left(Error.IllegalTransition(status, next))
  }

  def total: ARS.Price = items.map(i => i.article.self.price * i.amount.self).reduce(_ + _)

case class Item(article: Ident[Article], amount: Nat)

object Order: // TODO NonEmpty L I S T
  def apply(items: NonEmptySet[Item], customer: Customer): Order = new Order(items, customer)

enum Status:
  case Open, Paid, Delivered, Closed, Cancelled

class Store(val articles: Database[Article], val orders: Database[Order])

class StockManager(stock: Database[Stock]):

  def init(id: UUID, amount: Nat0 = Nat0.ZERO): ZIO[Any, Error, Ident[Stock]] =
    stock.insert(id, Stock(amount, Nat0.ZERO))

  def updateFor(order: Order): ZIO[Any, Error, List[Ident[Stock]]] =
    val noop: (UUID, Any) => ZIO[Any, Error, Ident[Stock]] = (id, _) => of(id)

    val fs = order.status match {
      case Status.Open                      => compromise
      case Status.Delivered | Status.Closed => release
      case Status.Cancelled                 => uncompromise
      case Status.Paid                      => noop
    }

    ZIO
      .collectAll(
        order.items.self
          .groupMapReduce(_.article.id)(_.amount)(_ + _)
          .map(fs.tupled)
      )
      .map(_.toList)

  def of(id: UUID): ZIO[Any, Error, Ident[Stock]] = stock.find(id)

  def increment(id: UUID, amount: Nat = Nat.ONE): ZIO[Any, Error, Ident[Stock]] = stock.update(id, _.increment(amount))

  def compromise(id: UUID, amount: Nat = Nat.ONE): ZIO[Any, Error, Ident[Stock]] =
    stock.update(id, _.compromise(amount))

  def uncompromise(id: UUID, amount: Nat = Nat.ONE): ZIO[Any, Error, Ident[Stock]] =
    stock.update(id, _.uncompromise(amount))

  def release(id: UUID, amount: Nat = Nat.ONE): ZIO[Any, Error, Ident[Stock]] = stock.update(id, _.release(amount))

case class Stock(inStock: Nat0, compromised: Nat0):

  def increment(amount: Nat): Stock = this.copy(inStock = inStock + amount)

  def compromise(amount: Nat): Stock = this.copy(compromised = compromised + amount)

  def uncompromise(amount: Nat): Stock = this.copy(compromised = Nat0(compromised - amount).getOrElse(Nat0.ZERO))

  def release(amount: Nat): Stock =
    Stock(Nat0(inStock - amount).getOrElse(Nat0.ZERO), Nat0(compromised - amount).getOrElse(Nat0.ZERO))

  def balance: Int = inStock - compromised

case class AddArticleForm(title: NonEmptyString, subtitle: String, price: ARS.Price)
case class UpdateArticlePriceForm(id: UUID, newPrice: ARS.Price)
case class PlaceOrderForm(items: NonEmptySet[Item], customer: Customer)
case class OverwriteStockForm(id: UUID, amount: Nat0)
case class IncrementStockForm(id: UUID, amount: Nat)

case class ArticleFilter(
  text: Option[String],
  priceRange: Option[Range[ARS.Price]],
) extends Filter[Article]:
  override def contains(article: Article): Boolean =
    (text.map(text => article.title.self.contains(text) || article.subtitle.contains(text))
      :: priceRange.map(_.contains(article.price))
      :: Nil)
      .collect[Boolean] { case Some(v) => v }
      .forall(identity)

type Name        = String
type ContactInfo = String
type Address     = String
