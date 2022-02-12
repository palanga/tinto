package core

import zio.{Random, ZIO, Accessible}

import java.util.UUID

trait StoreManager:
  def addArticle(form: AddArticleForm): ZIO[Random, Error, Ident[Article]]
  def updateArticlePrice(form: UpdateArticlePriceForm): ZIO[Any, Error, Ident[Article]]
  def removeArticle(id: UUID): ZIO[Any, Error, Ident[Article]]
  def listAllArticles: ZIO[Any, Error, List[Ident[Article]]]
  def placeOrder(form: PlaceOrderForm): ZIO[Random, Error, Ident[Order]]
  def markAsPaid(orderId: UUID): ZIO[Any, Error, Ident[Order]]
  def markAsDelivered(orderId: UUID): ZIO[Any, Error, Ident[Order]]
  def markAsCancelled(orderId: UUID): ZIO[Any, Error, Ident[Order]]
  def listAllOrders: ZIO[Any, Error, List[Ident[Order]]]
  def stock(id: UUID): ZIO[Any, Error, Ident[Stock]]
  def overwriteStock(form: OverwriteStockForm): ZIO[Any, Error, Ident[Stock]]
  def incrementStock(form: IncrementStockForm): ZIO[Any, Error, Ident[Stock]]

object StoreManager extends Accessible[StoreManager]
