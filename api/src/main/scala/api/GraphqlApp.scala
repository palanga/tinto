package api

import caliban.*
import caliban.GraphQL.graphQL
import caliban.schema.{ArgBuilder, GenericSchema, Schema}
import caliban.wrappers.ApolloTracing.apolloTracing
import caliban.wrappers.Wrappers.*
import core.*
import zhttp.http.*
import zhttp.service.Server
import zio.*
import zio.clock.Clock
import zio.console.Console
import zio.duration.*
import zio.random.Random

import java.util.UUID
import scala.language.postfixOps

object CalibanApp extends App:

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    (for {
      storeManager <- ZIO.environment[Has[StoreManager]]
      interpreter  <- Api.api(storeManager.get).interpreter
      _            <- addTestData(interpreter)
      _ <- Server
             .start(
               8088,
               Http.route { case _ -> Root / "api" / "graphql" =>
                 CORS(ZHttpAdapter.makeHttpService(interpreter))
               },
             )
             .forever
    } yield ())
      .provideCustomLayer(testLayer)
      .provideCustomLayer(zio.test.environment.TestRandom.deterministic)
      .exitCode

object Api extends GenericSchema[Any]:

  def api(storeManager: StoreManager): GraphQL[Console with Clock with Random] =
    (catalogApi(storeManager) |+| ordersApi(storeManager) |+| stockApi(storeManager))
    @@ maxFields (200)
    @@ maxDepth (30)
    @@ timeout (3 seconds)
    @@ printSlowQueries (500 millis)
    @@ printErrors

  private def catalogApi(storeManager: StoreManager) = graphQL(catalog.resolver(storeManager))
  private def ordersApi(storeManager: StoreManager)  = graphQL(orders.resolver(storeManager))
  private def stockApi(storeManager: StoreManager)   = graphQL(stock.resolver(storeManager))

  given Schema[Any, catalog.CatalogQueries]      = Schema.gen
  given Schema[Random, catalog.CatalogMutations] = Schema.gen
  given Schema[Any, orders.OrdersQueries]        = Schema.gen
  given Schema[Random, orders.OrdersMutations]   = Schema.gen
  given Schema[Any, stock.StockQueries]          = Schema.gen
  given Schema[Any, stock.StockMutations]        = Schema.gen

  given Schema[Any, ARS.Price]                                 = Schema.stringSchema.contramap(_.toString)
  given Schema[Any, Article]                                   = Schema.gen
  given Schema[Any, Order]                                     = Schema.gen
  given Schema[Any, Stock]                                     = Schema.gen
  given Schema[Any, Error]                                     = Schema.gen
  given Schema[Any, Nat]                                       = Schema.intSchema.contramap(_.self)
  given Schema[Any, Nat0]                                      = Schema.intSchema.contramap(_.self)
  given Schema[Any, NonEmptyString]                            = Schema.stringSchema.contramap(_.self)
  given IdentOfArticle: Schema[Any, Ident[Article]]            = Schema.gen
  given IdentOfOrder: Schema[Any, Ident[Order]]                = Schema.gen
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

val testLayer =
  (InMemoryDatabase.init[Article].toLayer
    ++ InMemoryDatabase.init[Order].toLayer
    ++ InMemoryDatabase.init[Stock].toLayer) >>> StoreManager.build.toLayer

def addTestData(interpreter: GraphQLInterpreter[Console with Clock with Random, CalibanError]) =
  for {
    _ <- interpreter.execute(queries.allArticles)
    _ <- interpreter.execute(queries.addArticle)
    _ <- interpreter.execute(queries.addArticleToRemove)
    _ <- interpreter.execute(queries.removeArticle)
    _ <- interpreter.execute(queries.addArticle2)
    _ <- interpreter.execute(queries.updatePrice)
    _ <- interpreter.execute(queries.writeStock)
    _ <- interpreter.execute(queries.incrementStock)
    _ <- interpreter.execute(queries.getStock)
    _ <- interpreter.execute(queries.placeOrder)
    _ <- interpreter.execute(queries.getStock)
    _ <- interpreter.execute(queries.placeOrderToCancel)
    _ <- interpreter.execute(queries.payOrder)
    _ <- interpreter.execute(queries.cancelOrder)
  } yield ()

object queries:

  val allArticles =
    """
      |query articles {
      |  articles {
      |    id
      |    self {
      |      title
      |      subtitle
      |      price
      |    }
      |  }
      |}
      |""".stripMargin

  val addArticle =
    """
      |mutation add {
      |  addArticle(title: "Potrillos", subtitle: "malbec", price: "ARS 800") {
      |    id
      |    self {
      |      title
      |      subtitle
      |      price
      |    }
      |  }
      |}
      |""".stripMargin

  val addArticleToRemove =
    """
      |mutation addBorrar {
      |  addArticle(title: "Potrillos", subtitle: "malbec", price: "ARS 800") {
      |    id
      |  }
      |}
      |""".stripMargin

  val addArticle2 =
    """
      |mutation add2 {
      |  addArticle(title: "Potrillos", subtitle: "pinot noir", price: "ARS 800") {
      |    id
      |  }
      |}
      |""".stripMargin

  val updatePrice =
    """
      |mutation update {
      |  updatePrice(id: "b2c8ccb8-191a-4233-9b34-3e3111a4adaf", newPrice: "ARS 900") {
      |    id
      |    self {
      |      price
      |    }
      |  }
      |}
      |""".stripMargin

  val removeArticle =
    """
      |mutation remove {
      |  removeArticle(value: "014a363e-7a00-48d5-b154-dc024003f3d1") {
      |    id
      |    self {
      |      title
      |      subtitle
      |    }
      |  }
      |}
      |""".stripMargin

  val getAllOrders =
    """
      |query orders {
      |  orders {
      |    id
      |    self {
      |      items {
      |        article {
      |          self {
      |            title
      |          	subtitle
      |          	price
      |          }
      |        }
      |        amount
      |      }
      |      customer {
      |        name
      |        contactInfo
      |        address
      |      }
      |      status
      |    }
      |  }
      |}
      |""".stripMargin

  val placeOrder =
    """
      |mutation place {
      |  placeOrder(
      |    items: [{article: {id: "b2c8ccb8-191a-4233-9b34-3e3111a4adaf", self: {title: "Potrillos", subtitle: "pinot noir", price: "ARS 900"}}, amount: 2}],
      |    customer: {
      |      name: "Martita",
      |      contactInfo: "martita@gmail.com",
      |      address: "Paseo 150 y Avenida 2"
      |    }
      |  ) {
      |    id
      |    self {
      |      items {
      |        article {
      |          id
      |          self {
      |            title
      |            subtitle
      |            price
      |          }
      |        }
      |      }
      |      status
      |      customer {
      |        name
      |        contactInfo
      |        address
      |      }
      |    }
      |  }
      |}
      |""".stripMargin

  val placeOrderToCancel =
    """
      |mutation place {
      |  placeOrder(
      |    items: [{article: {id: "b2c8ccb8-191a-4233-9b34-3e3111a4adaf", self: {title: "Potrillos", subtitle: "pinot noir", price: "ARS 900"}}, amount: 2}],
      |    customer: {
      |      name: "Martita",
      |      contactInfo: "martita@gmail.com",
      |      address: "Paseo 150 y Avenida 2"
      |    }
      |  ) {
      |    id
      |  }
      |}
      |""".stripMargin

  val payOrder =
    """
      |mutation pay {
      |  markAsPaid(value: "1c6c51c9-3d99-4619-889b-73a6c44ca06c") {
      |    id
      |    self {
      |      status
      |    }
      |  }
      |}
      |""".stripMargin

  val deliverOrder =
    """
      |mutation deliver {
      |  markAsDelivered(value: "1c6c51c9-3d99-4619-889b-73a6c44ca06c") {
      |    id
      |    self {
      |      status
      |    }
      |  }
      |}
      |""".stripMargin

  val cancelOrder =
    """
      |mutation cancel {
      |  markAsCancelled(value: "0d3d6d22-75c4-43f1-bf57-44679ed56d65") {
      |    id
      |  }
      |}
      |""".stripMargin

  val getStock =
    """
      |query stock {
      |  stock(value: "b2c8ccb8-191a-4233-9b34-3e3111a4adaf") {
      |    id
      |    self {
      |      inStock
      |      compromised
      |    }
      |  }
      |}
      |""".stripMargin

  val writeStock =
    """
      |mutation stockWrite {
      |  overwriteStock(id: "b2c8ccb8-191a-4233-9b34-3e3111a4adaf", amount: 12) {
      |    id
      |    self {
      |      inStock
      |      compromised
      |    }
      |  }
      |}
      |""".stripMargin

  val incrementStock =
    """
      |mutation stockIncrement {
      |  incrementStock(id: "b2c8ccb8-191a-4233-9b34-3e3111a4adaf", amount: 6) {
      |    id
      |    self {
      |      inStock
      |      compromised
      |    }
      |  }
      |}
      |""".stripMargin
