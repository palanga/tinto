package backend

import core.*
import endpoints.EndpointWithResolver
import web.*
import zhttp.http.HttpApp
//import zhttp.http.middleware.Cors.CorsConfig
import zhttp.http.Middleware.cors
import zhttp.service.{ChannelFactory, EventLoopGroup, Server}
import zhttp.service.server.ServerChannelFactory
import zio.ZIO
import zio.Console.printLine

object App extends zio.App:

  val fullApi = List(
    api.catalog.all.resolveWith(_ => core.LocalStoreManager.build.flatMap(_.listAllArticles).orDie),
    api.catalog.add.resolveWith(f => core.LocalStoreManager.build.flatMap(_.addArticle(f)).orDie),
    api.orders.place.resolveWith(order => core.LocalStoreManager.build.flatMap(_.placeOrder(order)).orDie),
  )

  val httpApp = fullApi.map(server.v4.asZHTTP(errorMapper)).reduce(_ ++ _)
  val docs    = fullApi.sortBy(_.endpoint.route.toString).map(_.endpoint.docs).mkString("\n")

  val port      = 8080
  val appServer = Server.port(port) ++ Server.app(httpApp /*@@ cors(CorsConfig(true))*/ )

//  val testLayer =
//    InMemoryDatabase.init[Article].toLayer
//      ++ InMemoryDatabase.init[Order].toLayer
//      ++ InMemoryDatabase.init[Stock].toLayer

  override def run(args: List[String]) =
    appServer.make
      .use(_ => printLine(docs) *> printLine(s"Server started on port $port") *> ZIO.never)
      .provideCustomLayer(
        zhttp.service.server.ServerChannelFactory.auto
          ++ zhttp.service.EventLoopGroup.auto(1)
//          ++ zhttp.service.ChannelFactory.auto
          ++ InMemoryDatabase.init[Article].toLayer
          ++ InMemoryDatabase.init[Order].toLayer
          ++ InMemoryDatabase.init[Stock].toLayer
      )
//      .provideCustomLayer(zio.test.environment.TestRandom.deterministic)
      .exitCode

  private def errorMapper(t: Throwable): zhttp.http.HttpError = t match {
    case e: zhttp.http.HttpError => e
    case e                       => zhttp.http.HttpError.InternalServerError(e.getMessage, Some(e.getCause))
  }
