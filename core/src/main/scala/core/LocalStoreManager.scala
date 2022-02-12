package core

import zio.Random.nextUUID
import zio.{Random, ZIO}

import java.util.UUID

class LocalStoreManager(store: Store, stock: StockManager) extends StoreManager:
  def addArticle(form: AddArticleForm): ZIO[Random, Error, Ident[Article]] =
    nextUUID
      .flatMap(store.articles.insert(_, Article(form.title, form.subtitle, form.price)))
      .flatMap(article => stock.init(article.id).as(article))

  def updateArticlePrice(form: UpdateArticlePriceForm): ZIO[Any, Error, Ident[Article]] =
    store.articles.update(form.id, old => old.copy(price = form.newPrice))

  def removeArticle(id: UUID): ZIO[Any, Error, Ident[Article]] = store.articles.delete(id)

  def listAllArticles: ZIO[Any, Error, List[Ident[Article]]] =
    store.articles.all.runCollect.map(_.toList) // TODO return chunk or stream

  def placeOrder(form: PlaceOrderForm): ZIO[Random, Error, Ident[Order]] =
    nextUUID
      .flatMap(store.orders.insert(_, Order(form.items, form.customer)))
      .flatMap(order => stock.updateFor(order.self).as(order))

  def markAsPaid(orderId: UUID): ZIO[Any, Error, Ident[Order]] =
    store.orders
      .updateEither(orderId, order => order.pay)
      .flatMap(order => stock.updateFor(order.self).as(order))

  def markAsDelivered(orderId: UUID): ZIO[Any, Error, Ident[Order]] =
    store.orders
      .updateEither(orderId, order => order.deliver)
      .flatMap(order => stock.updateFor(order.self).as(order))

  def markAsCancelled(orderId: UUID): ZIO[Any, Error, Ident[Order]] =
    store.orders
      .updateEither(orderId, order => order.cancel)
      .flatMap(order => stock.updateFor(order.self).as(order))

  def listAllOrders: ZIO[Any, Error, List[Ident[Order]]] = store.orders.all.runCollect.map(_.toList)

  def stock(id: UUID): ZIO[Any, Error, Ident[Stock]] = stock.of(id)

  def overwriteStock(form: OverwriteStockForm): ZIO[Any, Error, Ident[Stock]] = stock.init(form.id, form.amount)

  def incrementStock(form: IncrementStockForm): ZIO[Any, Error, Ident[Stock]] = stock.increment(form.id, form.amount)

object LocalStoreManager:
  val build: ZIO[Database[Order] with Database[Article] with Database[Stock], Nothing, StoreManager] =
    for {
      articles <- ZIO.environment[Database[Article]]
      orders   <- ZIO.environment[Database[Order]]
      stock    <- ZIO.environment[Database[Stock]]
    } yield LocalStoreManager(new Store(articles.get, orders.get), new StockManager(stock.get))
