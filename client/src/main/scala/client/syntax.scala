package client

//import io.netty.util.CharsetUtil.UTF_8
import sttp.capabilities
import web.{Endpoint, decodeJson}
import zio.Task
//import zhttp.http.{Headers, HttpData, URL}
//import zhttp.service.{ChannelFactory, Client, EventLoopGroup}
import zio.ZIO
import zio.json.JsonEncoder

import sttp.client3.*
//import org.asynchttpclient.{AsyncHttpClient, DefaultAsyncHttpClient}
import zio.Runtime
//import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend

object syntax:

//  AsyncHttpClientZioBackend.managed().use { backend => ??? }

//  val asyncHttpClient: AsyncHttpClient = DefaultAsyncHttpClient()
//  val runtime: Runtime[Any]            = Runtime.default
//  val backend: SttpBackend[Task, Any]  = AsyncHttpClientZioBackend.usingClient(runtime, asyncHttpClient)

//  val backend: SttpBackend[scala.concurrent.Future, Any] = FetchBackend()

  extension [In, Out](self: Endpoint[In, Out])
    def fetch(input: In): ZIO[zio.Has[SttpBackend[scala.concurrent.Future, Any]], Throwable, Out] =
      (for {
        // TODO cambiar a un tipo propio de backend para asegurarme de que sea creado con el contexto de ejecucion que va a usar ZIO fromFutureInterrupt
        backend <- ZIO.environment[zio.Has[SttpBackend[scala.concurrent.Future, Any]]]
        uri     <- ZIO fromEither sttp.model.Uri.parse(s"http://localhost:8080/${self.route}").left.map(new Exception(_))
        response <- ZIO fromFutureInterrupt { _ =>
                      sttp.client3.basicRequest
                        .post(uri)
                        .body(self.inCodec.encoder.encodeJson(input, None).toString)
                        .send(backend.get)
                    }
      } yield response.body.flatMap(self.outCodec.decoder.decodeJson).left.map(new Exception(_))).absolve

//      ZIO
//        .fromFuture(implicit ec =>
//          sttp.client3.basicRequest
//            .get(
//              sttp.model.Uri.parse(s"http://localhost:8080/${self.route}").fold(e => throw new Exception(e), identity)
//            )
//            .body(self.inCodec.encoder.encodeJson(input, None).toString)
//            .send(backend)
//        )
//        .map(_.body.left.map(new Exception(_)))
//        .absolve
//        .map(self.outCodec.decoder.decodeJson(_).left.map(new Exception(_)))
//        .absolve

//      ???

//    def fetch(input: In): ZIO[EventLoopGroup & ChannelFactory, Throwable, Out] =
//      ZIO
//        .fromEither(URL.fromString("http://localhost:8080/" + self.route))
//        .flatMap(Client.request(convert(self.method), _, Headers.empty, inputToJsonData(self.inCodec.encoder)(input)))
//        .flatMap(_.getBodyAsString)
//        .map(decodeJson[Out](self.outCodec.decoder))
//        .absolve

//  private def inputToJsonData[A](encoder: JsonEncoder[A])(input: A) =
//    HttpData.fromString(encoder.encodeJson(input, None).toString, UTF_8)

//  private def convert(method: web.Method) = method match {
//    case web.Method.GET  => zhttp.http.Method.GET
//    case web.Method.POST => zhttp.http.Method.POST
//  }
