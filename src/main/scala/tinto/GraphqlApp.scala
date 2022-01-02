package tinto

import caliban.GraphQL.graphQL
import caliban.schema.{ArgBuilder, GenericSchema, Schema}
import caliban.wrappers.ApolloTracing.apolloTracing
import caliban.wrappers.Wrappers.*
import caliban.{CalibanError, GraphQL, RootResolver, ZHttpAdapter}
import zhttp.http.*
import zhttp.service.Server
import zio.clock.Clock
import zio.console.Console
import zio.duration.*
import zio.random.Random
import zio.*

import java.util.UUID
import scala.deriving.Mirror
import scala.language.postfixOps

object CalibanApp extends App:

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    (for {
      storeManager <- ZIO.environment[Has[StoreManager]]
      interpreter  <- Api.api(storeManager.get).interpreter
      _ <- Server
             .start(
               8088,
               Http.route { case _ -> Root / "api" / "graphql" =>
                 CORS(ZHttpAdapter.makeHttpService(interpreter))
               },
             )
             .forever
    } yield ())
      .provideCustomLayer(
        (InMemoryDatabase.init[Article].toLayer
          ++ InMemoryDatabase.init[Order].toLayer
          ++ InMemoryDatabase.init[Stock].toLayer) >>> StoreManager.build.toLayer
      )
      .exitCode

object Api extends GenericSchema[Any]:

  case class Queries(
    articles: ZIO[Any, Error, List[Ident[Article]]],
    orders: ZIO[Any, Error, List[Ident[Order]]],
    stock: UUID => ZIO[Any, Error, Ident[Stock]],
  )

  case class Mutations(
    addArticle: AddArticleForm => ZIO[Random, Error, Ident[Article]],
    updatePrice: UpdateArticlePriceForm => ZIO[Any, Error, Ident[Article]],
    removeArticle: UUID => ZIO[Any, Error, Ident[Article]],
    placeOrder: PlaceOrderForm => ZIO[Random, Error, Ident[Order]],
    markAsPaid: UUID => ZIO[Any, Error, Ident[Order]],
    markAsDelivered: UUID => ZIO[Any, Error, Ident[Order]],
    markAsCancelled: UUID => ZIO[Any, Error, Ident[Order]],
    overwriteStock: OverwriteStockForm => ZIO[Any, Error, Ident[Stock]],
    incrementStock: IncrementStockForm => ZIO[Any, Error, Ident[Stock]],
  )

  def api(storeManager: StoreManager) =
    graphQL(
      RootResolver(
        Queries(
          storeManager.listAllArticles,
          storeManager.listAllOrders,
          storeManager.stock,
        ),
        Mutations(
          storeManager.addArticle,
          storeManager.updateArticlePrice,
          storeManager.removeArticle,
          storeManager.placeOrder,
          storeManager.markAsPaid,
          storeManager.markAsDelivered,
          storeManager.markAsCancelled,
          storeManager.overwriteStock,
          storeManager.incrementStock,
        ),
      )
    )
    @@ maxFields (200)               // query analyzer that limit query fields
    @@ maxDepth (30)                 // query analyzer that limit query depth
    @@ timeout (3 seconds)           // wrapper that fails slow queries
    @@ printSlowQueries (500 millis) // wrapper that logs slow queries
    @@ printErrors                   // wrapper that logs errors
//      @@ apolloTracing               // wrapper for https://github.com/apollographql/apollo-tracing

  given Schema[Any, Queries]                                   = Schema.gen
  given Schema[Random, Mutations]                              = Schema.gen
  given Schema[Any, ARS.Price]                                 = Schema.stringSchema.contramap(_.toString)
  given Schema[Any, Article]                                   = Schema.gen
  given Schema[Any, Order]                                     = Schema.gen
  given Schema[Any, Stock]                                     = Schema.gen
  given Schema[Any, Error]                                     = Schema.gen
  given Schema[Any, Nat]                                       = Schema.intSchema.contramap(_.self)
  given Schema[Any, Nat0]                                      = Schema.intSchema.contramap(_.self)
  given Schema[Any, NonEmptyString]                            = Schema.stringSchema.contramap(_.self)
  given [T](using Schema[Any, T]): Schema[Any, NonEmptySet[T]] = Schema.setSchema[Any, T].contramap(_.self)

  given ArgBuilder[ARS.Price] =
    ArgBuilder.string
      .flatMap(input =>
        ARS.fromString(input).toRight(new IllegalArgumentException(input)).left.map(toCalibanExecutionError)
      )

  given ArgBuilder[Nat]            = ArgBuilder.int.flatMap(Nat(_).left.map(toCalibanExecutionError))
  given ArgBuilder[Nat0]           = ArgBuilder.int.flatMap(Nat0(_).left.map(toCalibanExecutionError))
  given ArgBuilder[NonEmptyString] = ArgBuilder.string.flatMap(NonEmptyString(_).left.map(toCalibanExecutionError))
  given [T: ArgBuilder]: ArgBuilder[NonEmptySet[T]] =
    ArgBuilder.list[T].flatMap(l => NonEmptySet(l*).left.map(toCalibanExecutionError))

  private def toCalibanExecutionError(t: Throwable): CalibanError.ExecutionError =
    CalibanError.ExecutionError(Option(t.getMessage).getOrElse("no message"), innerThrowable = Some(t))

//query articles {
//  articles {
//    id
//    value {
//      title
//      subtitle
//      price
//    }
//  }
//}
//
//mutation add {
//  addArticle(title: "potrillos", subtitle: "malbec", price: "ARS 800")
//}
//
//mutation update {
//  updatePrice(id: "cb00349b-932e-451a-9ec4-b47e8399ccbc", newPrice: "ARS 900")
//}
//
//mutation remove {
//  removeArticle(value: "cb00349b-932e-451a-9ec4-b47e8399ccbc")
//}
//
//
//query orders {
//  orders {
//    id
//    value {
//      items {
//        article {
//          title
//          subtitle
//          price
//        }
//        amount
//      }
//      customer {
//        name
//        contactInfo
//        address
//      }
//      status
//    }
//  }
//}
//
//mutation place {
//  placeOrder(
//    items: [{article: {title: "potrillos", subtitle: "pinot noir", price: "ARS 900"}, amount: 2}],
//  customer: {
//    name: "martita",
//    contactInfo: "",
//    address: ""
//  }
//  )
//}
//
//mutation pay {
//  markAsPaid(value: "1b339f23-1d2c-4c08-8e7b-0e591b1f9ef5")
//}
//
//mutation deliver {
//  markAsDelivered(value: "1b339f23-1d2c-4c08-8e7b-0e591b1f9ef5")
//}
//
