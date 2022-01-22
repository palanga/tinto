package backend

import core.*
import endpoints.EndpointWithResolver
import web.*
import zhttp.http.{CORSConfig, HttpApp}
import zhttp.http.Middleware.cors
import zhttp.service.{ChannelFactory, EventLoopGroup, Server}
import zhttp.service.server.ServerChannelFactory
import zio.{Has, ZIO}
import zio.console.putStrLn

object App extends zio.App:

  val fullApi = List(
    api.catalog.all.resolveWith(_ => core.StoreManager.build.flatMap(_.listAllArticles).orDie),
    api.catalog.add.resolveWith(f => core.StoreManager.build.flatMap(_.addArticle(f)).orDie),
    api.orders.place.resolveWith(order => core.StoreManager.build.flatMap(_.placeOrder(order)).orDie),
  )

  val httpApp = fullApi.map(server.v4.asZHTTP(errorMapper)).reduce(_ ++ _)
  val docs    = fullApi.sortBy(_.endpoint.route.toString).map(_.endpoint.docs).mkString("\n")

  val port      = 8080
  val appServer = Server.port(port) ++ Server.app(httpApp @@ cors(CORSConfig(true)))

//  val testLayer =
//    InMemoryDatabase.init[Article].toLayer
//      ++ InMemoryDatabase.init[Order].toLayer
//      ++ InMemoryDatabase.init[Stock].toLayer

  override def run(args: List[String]) =
    appServer.make
      .use(_ => putStrLn(docs) *> putStrLn(s"Server started on port $port") *> ZIO.never)
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
