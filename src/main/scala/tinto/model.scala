package tinto

import aconcagua.price.mono.{Currency, Prices}
import zio.ZIO
import zio.random.{Random, nextUUID}

import java.util.UUID

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
    case Status.Paid      => Left(Error.AlreadyPaid)
    case Status.Closed    => Left(Error.OrderClosed)
  }

  def deliver: Either[Error, Order] = status match {
    case Status.Open      => Right(this.copy(status = Status.Delivered))
    case Status.Paid      => Right(this.copy(status = Status.Closed))
    case Status.Delivered => Left(Error.AlreadyDelivered)
    case Status.Closed    => Left(Error.OrderClosed)
  }

  def total: ARS.Price = items.map(i => i.article.price * i.amount.self).reduce(_ + _)

case class Item(article: Article, amount: Natural)

object Order:
  def apply(items: NonEmptySet[Item], customer: Customer): Order = new Order(items, customer)

enum Status:
  case Open, Paid, Delivered, Closed

class StoreManager(store: Store):
  def addArticle(form: AddArticleForm): ZIO[Random, Error, UUID] =
    nextUUID.flatMap(store.articles.create(_, Article(form.title, form.subtitle, form.price)))

  def updateArticlePrice(form: UpdateArticlePriceForm): ZIO[Any, Error, UUID] =
    store.articles.update(form.id, old => old.copy(price = form.newPrice))

  def removeArticle(id: UUID): ZIO[Any, Error, Unit] = store.articles.delete(id)

  def listAllArticles: ZIO[Any, Error, List[(UUID, Article)]] =
    store.articles.all.runCollect.map(_.toList) // TODO return chunk or stream

  def placeOrder(form: PlaceOrderForm): ZIO[Random, Error, UUID] =
    nextUUID.flatMap(store.orders.create(_, Order(form.items, form.customer)))

  def markAsPaid(orderId: UUID): ZIO[Any, Error, UUID] = store.orders.updateEither(orderId, order => order.pay)

  def markAsDelivered(orderId: UUID): ZIO[Any, Error, UUID] =
    store.orders.updateEither(orderId, order => order.deliver)

  def listAllOrders: ZIO[Any, Error, List[(UUID, Order)]] = store.orders.all.runCollect.map(_.toList)

class Store(val articles: Database[Article], val orders: Database[Order])

case class AddArticleForm(title: NonEmptyString, subtitle: String, price: ARS.Price)
case class UpdateArticlePriceForm(id: UUID, newPrice: ARS.Price)
case class PlaceOrderForm(items: NonEmptySet[Item], customer: Customer)

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

opaque type NonEmptyString = String
object NonEmptyString:
  def apply(s: String): Either[Error, NonEmptyString] = if s.isBlank then Left(Error.EmptyTitle) else Right(s)
  extension (t: NonEmptyString) def self: String      = t

opaque type NonEmptyList[T] = List[T]
object NonEmptyList:
  def apply[T](head: T, tail: T*): NonEmptyList[T] = head :: List(tail*)
  extension [A](list: NonEmptyList[A])
    def self: List[A]                                        = list
    def map[B](f: A => B): NonEmptyList[B]                   = list.map(f)
    def flatMap[B](f: A => NonEmptyList[B]): NonEmptyList[B] = list.flatMap(f)

opaque type NonEmptySet[T] = Set[T]
object NonEmptySet:
  def apply[T](head: T, tail: T*): NonEmptySet[T] = Set(tail*) + head
  def apply[T](elems: T*): Either[Error, NonEmptySet[T]] =
    if elems.isEmpty then Left(Error.EmptyItemList) else Right(NonEmptySet(elems.head, elems.tail*))
  extension [A](set: NonEmptySet[A])
    def self: Set[A]                                       = set
    def map[B](f: A => B): NonEmptySet[B]                  = set.map(f)
    def flatMap[B](f: A => NonEmptySet[B]): NonEmptySet[B] = set.flatMap(f)
    def reduce(f: (A, A) => A): A                          = set.reduce(f)

opaque type Natural = Int
object Natural:
  def apply(n: Int): Either[Error, Natural] = if n <= 0 then Left(Error.LessOrEqualToZero) else Right(n)
  extension (a: Natural) def self: Int      = a

type Name        = String
type ContactInfo = String
type Address     = String
