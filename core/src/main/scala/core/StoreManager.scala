package core

import zio.{Random, ZIO, Accessible}

import java.util.UUID

trait StoreManager:
  def addArticle(form: AddArticleForm): ZIO[Random, Throwable, Ident[Article]]
  def updateArticlePrice(form: UpdateArticlePriceForm): ZIO[Any, Throwable, Ident[Article]]
  def removeArticle(id: UUID): ZIO[Any, Throwable, Ident[Article]]
  def listAllArticles: ZIO[Any, Throwable, List[Ident[Article]]]
  def placeOrder(form: PlaceOrderForm): ZIO[Random, Throwable, Ident[Order]]
  def markAsPaid(orderId: UUID): ZIO[Any, Throwable, Ident[Order]]
  def markAsDelivered(orderId: UUID): ZIO[Any, Throwable, Ident[Order]]
  def markAsCancelled(orderId: UUID): ZIO[Any, Throwable, Ident[Order]]
  def listAllOrders: ZIO[Any, Throwable, List[Ident[Order]]]
  def stock(id: UUID): ZIO[Any, Throwable, Ident[Stock]]
  def overwriteStock(form: OverwriteStockForm): ZIO[Any, Throwable, Ident[Stock]]
  def incrementStock(form: IncrementStockForm): ZIO[Any, Throwable, Ident[Stock]]

object StoreManager extends Accessible[StoreManager]
