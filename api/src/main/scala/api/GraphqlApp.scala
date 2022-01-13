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
import zio.stream.ZStream
import zio.blocking.Blocking
//import io.netty.handler.codec.http.{HttpHeaderNames, HttpHeaderValues}

import java.io.IOException
import java.nio.file.Paths
import java.util.UUID
import scala.language.postfixOps

object CalibanApp extends App:

  private val graphiql: Http[Any, Nothing, Any, Response.HttpResponse[Blocking, IOException]] =
    Http.succeed(
      Response.http(
        content = HttpData.fromStream(ZStream.fromResource("graphiql.html"))
//        headers = List(Header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_HTML))
      )
    )

  private val index: Http[Any, Nothing, Any, Response.HttpResponse[Blocking, Throwable]] =
    Http.succeed(
      Response.http(
        content = HttpData.fromStream(ZStream.fromFile(Paths.get("/Users/palan/code/tinto/frontend/indexserver.html")))
      )
    )

  private val mainjs: Http[Any, Nothing, Any, Response.HttpResponse[Blocking, Throwable]] =
    Http.succeed(
      Response.http(
        content = HttpData.fromStream(
          ZStream.fromFile(Paths.get("/Users/palan/code/tinto/frontend/target/scala-3.1.0/frontend-fastopt/main.js"))
        )
      )
    )

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    (for {
      storeManager <- ZIO.environment[Has[StoreManager]]
      interpreter  <- Api.api(storeManager.get).interpreter
      _            <- addTestData(interpreter)
      _ <- Server
             .start(
               8088,
               Http.route {
                 case _ -> Root / "api" / "graphql" => CORS(ZHttpAdapter.makeHttpService(interpreter))
                 case _ -> Root / "main.js"         => mainjs
                 case _                             => index
//                 case _ -> Root / "graphiql"        => graphiql
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
