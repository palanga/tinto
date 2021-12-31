package tinto

import zio.test.*
import zio.test.Assertion.*

// TODO todo
object graphqlTest extends DefaultMutableRunnableSpec:

  suite("graphql") {

    testM("value classes") {

      for {
        articles    <- InMemoryDatabase.init[Article]
        orders      <- InMemoryDatabase.init[Order]
        store        = Store(articles, orders)
        storeManager = StoreManager(store)
        interpreter <- Api.api(storeManager).interpreter
      } yield {

        val query =
          """
            |
            |""".stripMargin

//        interpreter.execute()

      }

      ???

//      case class Queries(events: List[Event], painters: List[WrappedPainter])
//      val event       = Event(OrganizationId(7), "Frida Kahlo exhibition")
//      val painter     = Painter("Claude Monet", "Impressionism")
//      val api         = tinto.Api.api()
//      val interpreter = api.interpreter
//      val query =
//        """query {
//          |  events {
//          |    organizationId
//          |    title
//          |  }
//          |  painters {
//          |    name
//          |    movement
//          |  }
//          |}""".stripMargin
//      assertM(interpreter.flatMap(_.execute(query)).map(_.data.toString))(
//        equalTo(
//          """{"events":[{"organizationId":7,"title":"Frida Kahlo exhibition"}],"painters":[{"name":"Claude Monet","movement":"Impressionism"}]}"""
//        )
//      )
    }

  }
