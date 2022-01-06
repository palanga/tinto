package api

import caliban.RootResolver
import core.*
import zio.ZIO

import java.util.UUID

object stock:

  def resolver(storeManager: StoreManager): RootResolver[StockQueries, StockMutations, Unit] =
    caliban.RootResolver(
      StockQueries(storeManager.stock),
      StockMutations(
        storeManager.overwriteStock,
        storeManager.incrementStock,
      ),
    )

  case class StockQueries(
    stock: UUID => ZIO[Any, Error, Ident[Stock]]
  )

  case class StockMutations(
    overwriteStock: OverwriteStockForm => ZIO[Any, Error, Ident[Stock]],
    incrementStock: IncrementStockForm => ZIO[Any, Error, Ident[Stock]],
  )
