package examples

import endpoints.*
import zhttp.http.CORSConfig
import zio.ZIO
import zio.clock.Clock
import zio.console.{Console, putStrLn}
import zio.json.{DeriveJsonCodec, JsonCodec}

object examplev4 extends zio.App:

  object api:
    val healthCheck: AnyUnitEndpoint   = Endpoint.get("healthcheck")
    val healthCheckV2: AnyUnitEndpoint = Endpoint.get("healthcheck/v2")
//    val echo: ParamsAnyUnitEndpoint[(String, Int)] = Endpoint.get(Route.init / "echo" / StringParam / IntParam)
    val echoa: ParamsOutEndpoint[String, String] = Endpoint.get(Route.init / "echo" / StringParam).out[String]
//  val book        = v4.Endpoint.get("book").out[Book]

//case class Book(name: String)
//
//given JsonCodec[Book] = DeriveJsonCodec.gen

  object app:
    import zio.duration.*
    import zhttp.service.Server
    import zhttp.http.Middleware.cors

    //    List[zhttp.http.HttpApp[zio.console.Console & zio.clock.Clock, Throwable]]
    val httpApp: zhttp.http.HttpApp[zio.console.Console & zio.clock.Clock, Throwable] = List(
      api.healthCheck.resolveWith(_ => putStrLn("ok")),
      api.healthCheckV2.resolveWith(_ => putStrLn("ok")),
//      api.echo.resolveWith { case ((text, delay), _) => ZIO.unit.delay(delay.seconds) },
      api.echoa.resolveWith { case (text, _) => ZIO.succeed(text) },
      //    api.book.resolveWith(_ => ZIO succeed Book("Rayuela")),
    ).map(server.v4.asZHTTP(errorMapper)).reduce(_ ++ _)

    def errorMapper(t: Throwable): zhttp.http.HttpError = t match {
      case e: zhttp.http.HttpError => e
      case e                       => zhttp.http.HttpError.InternalServerError(e.getMessage, Some(e.getCause))
    }
    val port      = 8080
    val appServer = Server.port(port) ++ Server.app(httpApp @@ cors(CORSConfig(true)))

  val docs = List(
    api.healthCheckV2,
    api.echoa,
    api.healthCheck,
  ).sortBy(_.route.toString).map(_.docs).mkString("\n")

  override def run(args: List[String]) =
    app.appServer.make
      .use(_ =>
        putStrLn(docs)
        *>
        putStrLn(s"Server started on port ${app.port}")
        //            *> fetching
          *> ZIO.never
      )
      .provideCustomLayer(
        zhttp.service.server.ServerChannelFactory.auto
          ++ zhttp.service.EventLoopGroup.auto(1)
          ++ zhttp.service.ChannelFactory.auto
      )
      .exitCode
