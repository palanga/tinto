import aconcagua.price.mono.Prices
import aconcagua.price.mono.Currency
import caliban.CalibanError
import caliban.schema.{ArgBuilder, Schema}
import zio.stm.{TMap, TSet, USTM, ZSTM}
import zio.stream.ZStream
import zio.{Chunk, UIO, ZIO}

import java.util.UUID
import scala.language.postfixOps
import scala.math.BigDecimal

object model:

  val ARS = Prices.ofCurrency(Currency.ARS)

  case class Article(
    title: Title,
    subtitle: Subtitle,
    price: ARS.Price,
  )

  case class Customer(
    name: Name,
    contactInfo: ContactInfo,
    address: Address,
  )

  case class Order private (
    items: Set[Item],
    customer: Customer,
    status: Status = Status.Open,
  ):

    def pay: ZIO[Any, Error, Order] = status match {
      case Status.Open      => ZIO succeed this.copy(status = Status.Paid)
      case Status.Delivered => ZIO succeed this.copy(status = Status.Closed)
      case Status.Paid      => ZIO fail Error.AlreadyPaid
      case Status.Closed    => ZIO fail Error.OrderClosed
    }

    def deliver: ZIO[Any, Error, Order] = status match {
      case Status.Open      => ZIO succeed this.copy(status = Status.Delivered)
      case Status.Paid      => ZIO succeed this.copy(status = Status.Closed)
      case Status.Delivered => ZIO fail Error.AlreadyDelivered
      case Status.Closed    => ZIO fail Error.OrderClosed
    }

  case class Item(article: Article, amount: Amount)

  object Order:
    def create(items: Set[Item], customer: Customer): ZIO[Any, Error, Order] =
      if items.isEmpty then ZIO fail Error.EmptyItemList else ZIO succeed Order(items, customer)

  enum Status:
    case Open, Paid, Delivered, Closed

  enum Error extends Throwable:
    case AlreadyPaid, AlreadyDelivered, OrderClosed, ContactInfoIsBlank, InvalidAddress, EmptyItemList, EmptyTitle,
    LessOrEqualToZero
    case NotFound(id: UUID)

    override def getMessage: String = this match {
      case AlreadyPaid        => "AlreadyPaid"
      case AlreadyDelivered   => "AlreadyDelivered"
      case OrderClosed        => "OrderClosed"
      case ContactInfoIsBlank => "ContactInfoIsBlank"
      case InvalidAddress     => "InvalidAddress"
      case EmptyItemList      => "EmptyItemList"
      case EmptyTitle         => "EmptyTitle"
      case LessOrEqualToZero  => "LessOrEqualToZero"
      case NotFound(id)       => s"Element with id <<$id>> not found"
    }

  opaque type Title = String
  extension (t: Title) def self: String = t
  object Title:
    def apply(s: String): Either[Error, Title] = if s.isBlank then Left(Error.EmptyTitle) else Right(s)

  type Subtitle = String

  opaque type Amount = Int
  extension (a: Amount) def self: Int = a
  object Amount:
    def apply(n: Int): Either[Error, Amount] = if n <= 0 then Left(Error.LessOrEqualToZero) else Right(n)

  type OrderId = UUID

  type Name        = String
  type ContactInfo = String
  type Address     = String

  class StoreManager(store: InMemoryStore):

    def addArticle(title: Title, subtitle: String, price: ARS.Price): ZIO[Any, Nothing, Unit] =
      store addArticle Article(title, subtitle, price) commit

    def updateArticlePrice(title: Title, newPrice: ARS.Price): ZIO[Any, Nothing, Unit] =
      store
        .removeArticle(title)
        .map(_.head)
        .flatMap(store addArticle _.copy(price = newPrice))
        .commit

    def removeArticle(title: Title): ZIO[Any, Nothing, Chunk[Article]] = store.removeArticle(title).commit

    def listAllArticles: ZIO[Any, Nothing, List[Article]] = store.articles.toList.commit

    def placeOrder(items: Set[Item], customer: Customer): ZIO[Any, Error, OrderId] =
      Order.create(items, customer).flatMap(store.addOrder(_).commit)

    def markAsPaid(orderId: OrderId): ZIO[Any, Error, OrderId] =
      def findOrder(orderId: OrderId)  = store.orders.get(orderId).map(_.get)                   // TODO can throw
      def markPaidAndAdd(order: Order) = order.pay.flatMap(store.orders.put(orderId, _).commit) // TODO do not commit
      for {
        order <- findOrder(orderId).commit // TODO commit at the end
        _ <- markPaidAndAdd(order)
      } yield orderId

    def markAsDelivered(orderId: OrderId): ZIO[Any, Error, OrderId] =
      def findOrder(orderId: OrderId) = store.orders.get(orderId).map(_.get) // TODO can throw
      def markDeliveredAndAdd(order: Order) =
        order.deliver.flatMap(store.orders.put(orderId, _).commit) // TODO do not commit
      for {
        order <- findOrder(orderId).commit // TODO commit at the end
        _ <- markDeliveredAndAdd(order)
      } yield orderId

    def listAllOrders: ZIO[Any, Nothing, List[(OrderId, Order)]] = store.orders.toList.commit

  class StoreManagerV2(store: Store)

  class Store(val articles: Database[Article], val orders: Database[Order])

  trait Database[T]:
    def create(id: UUID, element: T): ZIO[Any, Error, UUID]
    def update(id: UUID, newElement: T): ZIO[Any, Error, UUID]
    def delete(id: UUID): ZIO[Any, Error, Unit]
    def find(id: UUID): ZIO[Any, Error, T]
    def all: ZStream[Any, Error, (UUID, T)]
    def filter(filter: Filter[T]): ZStream[Any, Error, (UUID, T)]

  trait Filter[T]:
    def contains(element: T): Boolean

  case class ArticleFilter(
    text: Option[String],
    priceRange: Option[Range[ARS.Price]],
  ) extends Filter[Article]:
    override def contains(article: Article): Boolean =
      (text.map(text => article.title.contains(text) || article.subtitle.contains(text))
        :: priceRange.map(_.contains(article.price))
        :: Nil)
        .collect[Boolean] { case Some(v) => v }
        .forall(identity)

  trait Decode[T]:
    def decode(input: String): Option[T]

  class InMemoryDatabase[T](val elements: TMap[UUID, T]) extends Database[T]:
    override def create(id: UUID, element: T): ZIO[Any, Error, OrderId] =
      elements.put(id, element).commit.as(id)
    override def update(id: OrderId, newElement: T): ZIO[Any, Error, OrderId] =
      elements.merge(id, newElement)((_, _) => newElement).commit.as(id)
    override def delete(id: OrderId): ZIO[Any, Error, Unit] =
      elements.delete(id).commit.unit
    override def find(id: OrderId): ZIO[Any, Error, T] =
      elements.get(id).commit.someOrFail(Error.NotFound(id))
    override def all: ZStream[Any, Error, (UUID, T)] =
      ZStream.fromIterableM(elements.toChunk.commit)
    override def filter(filter: Filter[T]): ZStream[Any, Error, (UUID, T)] =
      all.filter((_, element) => filter.contains(element))

  object InMemoryDatabase:
    def init[T]: ZIO[Any, Nothing, InMemoryDatabase[T]] = TMap.empty[UUID, T].commit.map(InMemoryDatabase(_))

  class InMemoryStore(val articles: TSet[Article], val orders: TMap[OrderId, Order]):

    def addArticle(article: Article): USTM[Unit] = articles put article

    def removeArticle(title: Title): USTM[Chunk[Article]] =
      for {
        oldArticles <- articles.toList
        _           <- articles removeIf (_.title == title)
      } yield oldArticles.find(_.title == title).fold(Chunk.empty)(Chunk(_)) // TODO con zio 2 esto no es necesario

    def addOrder(order: Order): ZSTM[Any, Nothing, OrderId] =
      val orderId = UUID.randomUUID()
      orders.put(orderId, order).as(orderId)

    def removeOrder(orderId: OrderId): USTM[Unit] = orders.delete(orderId)
