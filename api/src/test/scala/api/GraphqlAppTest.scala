package api

import zio.test.Assertion.equalTo
import zio.test.{DefaultRunnableSpec, assert, assertM}
import zio.{Has, ZIO}
import core.*

object GraphqlAppTest extends DefaultRunnableSpec:

  private val interpreter =
    for {
      storeManager <- ZIO.environment[Has[StoreManager]]
      interpreter  <- Api.api(storeManager.get).interpreter
    } yield interpreter

  private def executeAndAssert(query: String)(expected: String) =
    assertM(interpreter.flatMap(_.execute(query)).map(_.data.toString))(equalTo(expected))

  private def execute(query: String) = interpreter.flatMap(_.execute(query))

  override def spec = fullSpec.provideCustomLayer(testLayer)

  private val fullSpec = suite("api")(
    testM("add article") {
      executeAndAssert(
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
      )(
        """{"addArticle":{"id":"b2c8ccb8-191a-4233-9b34-3e3111a4adaf","self":{"title":"Potrillos","subtitle":"malbec","price":"ARS 800"}}}"""
      )
    },
    testM("add, update, remove and fetch articles") {
      for {
        _        <- execute(queries.addArticle)
        _        <- execute(queries.addArticleToRemove)
        _        <- execute(queries.addArticle2)
        _        <- execute(queries.updatePrice)
        _        <- execute(queries.removeArticle)
        articles <- execute(queries.allArticles)
      } yield assert(articles.data.toString)(
        equalTo(
          """{"articles":[{"id":"03139315-09fa-4afc-8cc1-8afcb010903d","self":{"title":"Potrillos","subtitle":"pinot noir","price":"ARS 800"}},{"id":"b2c8ccb8-191a-4233-9b34-3e3111a4adaf","self":{"title":"Potrillos","subtitle":"malbec","price":"ARS 900"}}]}"""
        )
      )
    },
    testM("place, pay and deliver order") {
      for {
        _      <- execute(queries.addArticle)
        _      <- execute(queries.addArticleToRemove)
        _      <- execute(queries.addArticle2)
        _      <- execute(queries.updatePrice)
        _      <- execute(queries.placeOrder)
        _      <- execute(queries.placeOrderToCancel)
        _      <- execute(queries.payOrder)
        _      <- execute(queries.deliverOrder)
        _      <- execute(queries.cancelOrder)
        orders <- execute(queries.getAllOrders)
      } yield assert(orders.data.toString)(
        equalTo(
          """{"orders":[{"id":"1c6c51c9-3d99-4619-889b-73a6c44ca06c","self":{"items":[{"article":{"self":{"title":"Potrillos","subtitle":"pinot noir","price":"ARS 900"}},"amount":2}],"customer":{"name":"Martita","contactInfo":"martita@gmail.com","address":"Paseo 150 y Avenida 2"},"status":"Closed"}},{"id":"0d3d6d22-75c4-43f1-bf57-44679ed56d65","self":{"items":[{"article":{"self":{"title":"Potrillos","subtitle":"pinot noir","price":"ARS 900"}},"amount":2}],"customer":{"name":"Martita","contactInfo":"martita@gmail.com","address":"Paseo 150 y Avenida 2"},"status":"Cancelled"}}]}"""
        )
      )
    },
    testM("stock") {
      for {
        _                <- execute(queries.addArticle)
        _                <- execute(queries.addArticleToRemove)
        _                <- execute(queries.addArticle2)
        _                <- execute(queries.writeStock)
        _                <- execute(queries.incrementStock)
        initialStock     <- execute(queries.getStock)
        _                <- execute(queries.placeOrder)
        compromisedStock <- execute(queries.getStock)
        _                <- execute(queries.placeOrderToCancel)
        _                <- execute(queries.payOrder)
        _                <- execute(queries.deliverOrder)
        _                <- execute(queries.cancelOrder)
        finalStock       <- execute(queries.getStock)
      } yield assert(initialStock.data.toString)(
        equalTo("""{"stock":{"id":"b2c8ccb8-191a-4233-9b34-3e3111a4adaf","self":{"inStock":18,"compromised":0}}}""")
      )
        && assert(compromisedStock.data.toString)(
          equalTo("""{"stock":{"id":"b2c8ccb8-191a-4233-9b34-3e3111a4adaf","self":{"inStock":18,"compromised":2}}}""")
        )
        && assert(finalStock.data.toString)(
          equalTo("""{"stock":{"id":"b2c8ccb8-191a-4233-9b34-3e3111a4adaf","self":{"inStock":16,"compromised":0}}}""")
        )
    },
  )
