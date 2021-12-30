import caliban.GraphQL.graphQL
import caliban.schema.{ArgBuilder, GenericSchema, Schema}
import caliban.wrappers.ApolloTracing.apolloTracing
import caliban.wrappers.Wrappers.*
import caliban.{CalibanError, GraphQL, RootResolver, ZHttpAdapter}
import model.*
import zhttp.http.*
import zhttp.service.Server
import zio.clock.Clock
import zio.console.Console
import zio.duration.*
import zio.{ZEnv, *}

import scala.deriving.Mirror
import scala.language.postfixOps

object CalibanApp extends App:

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, ExitCode] =
    (for {
      articles    <- zio.stm.TSet.empty[Article].commit
      orders      <- zio.stm.TMap.empty[OrderId, Order].commit
      store        = model.InMemoryStore(articles, orders)
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
    } yield ())
//      .provideCustomLayer(ExampleService.make(sampleCharacters))
      .exitCode

object Api extends GenericSchema[Any]:

  case class Queries(
    articles: ZIO[Any, Nothing, List[Article]],
    orders: ZIO[Any, Nothing, List[(OrderId, Order)]],
  )

  case class Mutations(
    addArticle: AddArticleArgs => ZIO[Any, Nothing, Unit],
    updatePrice: UpdateArticlePriceArgs => ZIO[Any, Nothing, Unit],
    removeArticle: RemoveArticleArgs => ZIO[Any, Nothing, Unit],
    placeOrder: PlaceOrderArgs => ZIO[Any, Error, OrderId],
    markAsPaid: OrderId => ZIO[Any, Error, OrderId],
    markAsDelivered: OrderId => ZIO[Any, Error, OrderId],
  )

  given priceSchema: Schema[Any, ARS.Price] = Schema.stringSchema.contramap(_.toString)
  given priceArgBuilder: ArgBuilder[ARS.Price] =
    ArgBuilder.string
      .flatMap(input =>
        ARS.fromString(input).toRight(CalibanError.ExecutionError(new IllegalArgumentException(input).getMessage))
      )

  given articleSchema: Schema[Any, Article] = Schema.gen

  given amountSchema: Schema[Any, Amount] = Schema.intSchema.contramap(_.self)
  given amountArgBuilder: ArgBuilder[Amount] =
    ArgBuilder.int.flatMap(Amount(_).left.map(e => CalibanError.ExecutionError(e.getMessage)))

  given titleSchema: Schema[Any, Title] = Schema.stringSchema.contramap(_.self)
  given titleArgBuilder: ArgBuilder[Title] =
    ArgBuilder.string.flatMap(Title(_).left.map(e => CalibanError.ExecutionError(e.getMessage)))

  def api(storeManager: StoreManager) =
    graphQL(
      RootResolver(
        Queries(
          storeManager.listAllArticles,
          storeManager.listAllOrders,
        ),
        Mutations(
          args => storeManager.addArticle(args.title, args.subtitle, args.price),
          args => storeManager.updateArticlePrice(args.title, args.newPrice),
          args => storeManager.removeArticle(args.title).unit,
          args => storeManager.placeOrder(args.items.toSet, args.customer),
          storeManager.markAsPaid,
          storeManager.markAsDelivered,
        ),
      )
    )
//    @@ maxFields (200)               // query analyzer that limit query fields
//    @@ maxDepth (30)                 // query analyzer that limit query depth
//    @@ timeout (3 seconds)           // wrapper that fails slow queries
//    @@ printSlowQueries (500 millis) // wrapper that logs slow queries
    @@ printErrors // wrapper that logs errors
//      @@ apolloTracing               // wrapper for https://github.com/apollographql/apollo-tracing

  case class AddArticleArgs(title: Title, subtitle: Subtitle, price: ARS.Price)
  case class UpdateArticlePriceArgs(title: Title, newPrice: ARS.Price)
  case class RemoveArticleArgs(title: Title)
  case class PlaceOrderArgs(items: List[Item], customer: Customer)
  case class ArticleLineItem(title: Title, subtitle: Subtitle, price: Int, amount: Int)

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
