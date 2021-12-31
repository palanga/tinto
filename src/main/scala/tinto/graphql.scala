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
      articles    <- InMemoryDatabase.init[Article]
      orders      <- InMemoryDatabase.init[Order]
      store        = Store(articles, orders)
      storeManager = StoreManager(store)
      interpreter <- Api.api(storeManager).interpreter
      _ <- Server
             .start(
               8088,
               Http.route { case _ -> Root / "api" / "graphql" =>
                 CORS(ZHttpAdapter.makeHttpService(interpreter))
               },
             )
             .forever
    } yield ()).exitCode

object Api extends GenericSchema[Any]:

  case class Queries(
    articles: ZIO[Any, Error, List[(UUID, Article)]],
    orders: ZIO[Any, Error, List[(UUID, Order)]],
  )

  case class Mutations(
    addArticle: AddArticleForm => ZIO[Random, Error, UUID],
    updatePrice: UpdateArticlePriceForm => ZIO[Any, Error, UUID],
    removeArticle: UUID => ZIO[Any, Error, Unit],
    placeOrder: PlaceOrderForm => ZIO[Random, Error, UUID],
    markAsPaid: UUID => ZIO[Any, Error, UUID],
    markAsDelivered: UUID => ZIO[Any, Error, UUID],
  )

  def api(storeManager: StoreManager): GraphQL[Random] =
    graphQL(
      RootResolver(
        Queries(
          storeManager.listAllArticles,
          storeManager.listAllOrders,
        ),
        Mutations(
          storeManager.addArticle,
          storeManager.updateArticlePrice,
          storeManager.removeArticle,
          storeManager.placeOrder,
          storeManager.markAsPaid,
          storeManager.markAsDelivered,
        ),
      )
    )
//    @@ maxFields (200)               // query analyzer that limit query fields
//    @@ maxDepth (30)                 // query analyzer that limit query depth
//    @@ timeout (3 seconds)           // wrapper that fails slow queries
//    @@ printSlowQueries (500 millis) // wrapper that logs slow queries
//    @@ printErrors // wrapper that logs errors
//      @@ apolloTracing               // wrapper for https://github.com/apollographql/apollo-tracing

  given Schema[Any, Queries]                                   = Schema.gen
  given Schema[Random, Mutations]                              = Schema.gen
  given Schema[Any, ARS.Price]                                 = Schema.stringSchema.contramap(_.toString)
  given Schema[Any, Article]                                   = Schema.gen
  given Schema[Any, Order]                                     = Schema.gen
  given Schema[Any, Error]                                     = Schema.gen
  given Schema[Any, Natural]                                   = Schema.intSchema.contramap(_.self)
  given Schema[Any, NonEmptyString]                            = Schema.stringSchema.contramap(_.self)
  given [T](using Schema[Any, T]): Schema[Any, NonEmptySet[T]] = Schema.setSchema[Any, T].contramap(_.self)

  given ArgBuilder[ARS.Price] =
    ArgBuilder.string
      .flatMap(input =>
        ARS.fromString(input).toRight(new IllegalArgumentException(input)).left.map(toCalibanExecutionError)
      )

  given ArgBuilder[Natural]        = ArgBuilder.int.flatMap(Natural(_).left.map(toCalibanExecutionError))
  given ArgBuilder[NonEmptyString] = ArgBuilder.string.flatMap(NonEmptyString(_).left.map(toCalibanExecutionError))
  given [T: ArgBuilder]: ArgBuilder[NonEmptySet[T]] =
    ArgBuilder.list[T].flatMap(l => NonEmptySet(l*).left.map(toCalibanExecutionError))

  private def toCalibanExecutionError(t: Throwable): CalibanError.ExecutionError =
    CalibanError.ExecutionError(Option(t.getMessage).getOrElse("no message"), innerThrowable = Some(t))

//query wines {
//  wines {
//    name
//    strain
//    price
//  }
//}
//
//mutation add {
//  addWine(name: "potrillos", strain: "pinot noir", price: 800)
//}
//
//mutation update {
//  updatePrice(name: "potrillos", newPrice: 900)
//}
//
//mutation remove {
//  removeWine(name: "potrillos")
//}
//
//
//query orders {
//  orders {
//    _1
//    _2 {
//      wines {
//        key {
//          name
//          strain
//          price
//        }
//        value
//      }
//      contactInfo
//      status
//    }
//  }
//}
//
//mutation place {
//  placeOrder(wines: [{_1: {name: "potrillos", strain: "pinot noir", price: 900}, _2: 2}], contactInfo: "martita")
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

//import zio._
//import zio.console._

/**
 *   - cargar un vino con nombre, variedad y precio
 *   - actualizar el precio
 *   - poner que no esta disponible
 *   - borrar un vino
 *   - listar todos los vinos disponibles
 *
 *   - hacer un pedido de vinos (nombre por cantidad y datos de contacto del
 *     cliente)
 *   - marcar el pedido como pago
 *   - marcar el pedido como entregado
 *   - marcar el pedido como finalizado
 */
//object Main extends zio.App {
//
//  override def run(args: List[String]) = myAppLogic.exitCode
//
//  val myAppLogic =
//    for {
//      _    <- putStrLn("Hello! What is your name?")
//      name <- getStrLn
//      _    <- putStrLn(s"Hello, $name, welcome to ZIO!")
//    } yield ()
//
//}

//import model.{Order, UUID, Store, StoreManager, Article}
//
//import zio.*
//import zhttp.http.*
//import zhttp.service.Server

//object Main extends App {
//
//  val app = Http.collect[Request] {
//    case Method.GET -> Root           => Response.text("hola")
//    case Method.GET -> Root / "text"  => Response.text("hola")
//    case Method.GET -> Root / "wines" => Response.text("hola")
//  }
//
//  val deapp = Http.collectM[Request] { case Method.GET -> Root / "wines" =>
//    ZIO succeed Response.text("hola")
//  }
//
//  val winesApp =
//    for {
//      wines       <- zio.stm.TSet.empty[Wine]
//      orders      <- zio.stm.TMap.empty[UUID, Order]
//      store        = Store(wines, orders)
//      storeManager = StoreManager(store)
//    } yield web.routes.wines(storeManager)
//
//  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
////    winesApp.commit.flatMap(Server.start(8090, _).exitCode)
////    winesApp.commit.map(_.catchAll(t => Http.error(t.getMessage))).flatMap(Server.start(8090, _)).exitCode
//    Server.start(8090, deapp).exitCode
//}
