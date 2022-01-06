package api

import caliban.RootResolver
import core.*
import zio.ZIO
import zio.random.Random

import java.util.UUID

object orders:

  def resolver(storeManager: StoreManager): RootResolver[OrdersQueries, OrdersMutations, Unit] =
    RootResolver(
      OrdersQueries(storeManager.listAllOrders),
      OrdersMutations(
        storeManager.placeOrder,
        storeManager.markAsPaid,
        storeManager.markAsDelivered,
        storeManager.markAsCancelled,
      ),
    )

  case class OrdersQueries(
    orders: ZIO[Any, Error, List[Ident[Order]]]
  )

  case class OrdersMutations(
    placeOrder: PlaceOrderForm => ZIO[Random, Error, Ident[Order]],
    markAsPaid: UUID => ZIO[Any, Error, Ident[Order]],
    markAsDelivered: UUID => ZIO[Any, Error, Ident[Order]],
    markAsCancelled: UUID => ZIO[Any, Error, Ident[Order]],
  )
