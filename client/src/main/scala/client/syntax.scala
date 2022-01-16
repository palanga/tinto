package client

import sttp.capabilities
import sttp.client3.*
import sttp.model.Uri
import web.Endpoint
import zio.{ZIO, ZManaged}
import zio.json.JsonEncoder

import scala.concurrent.Future

object syntax:

  extension [In, Out](self: Endpoint[In, Out])
    // TODO cambiar la implementacion en futuro a una de cats effects porque futuro no se puede cancelar bien
    def fetch(input: In): ZIO[Any, Throwable, Out] =
      (for {
        uri <- ZIO fromEither Uri.parse(s"http://localhost:8080/${self.route}").left.map(new Exception(_))
        response <- makeBackend.use(backend =>
                      ZIO fromFuture { _ =>
                        sttp.client3.basicRequest
                          .method(convert(self.method), uri)
                          .body(self.inCodec.encoder.encodeJson(input, None).toString)
                          .send(backend)
                      }
                    )
      } yield response.body.flatMap(self.outCodec.decoder.decodeJson).left.map(new Exception(_))).absolve

  private def makeBackend: ZManaged[Any, Nothing, SttpBackend[Future, capabilities.WebSockets]] =
    ZIO.succeed(FetchBackend()).toManaged(backend => ZIO.fromFuture(_ => backend.close()).ignore)

  private def convert(method: web.Method) = method match {
    case web.Method.GET  => sttp.model.Method.GET
    case web.Method.POST => sttp.model.Method.POST
  }

//  AsyncHttpClientZioBackend.managed().use { backend => ??? }

//  val asyncHttpClient: AsyncHttpClient = DefaultAsyncHttpClient()
//  val runtime: Runtime[Any]            = Runtime.default
//  val backend: SttpBackend[Task, Any]  = AsyncHttpClientZioBackend.usingClient(runtime, asyncHttpClient)

//    def fetch(input: In): ZIO[EventLoopGroup & ChannelFactory, Throwable, Out] =
//      ZIO
//        .fromEither(URL.fromString("http://localhost:8080/" + self.route))
//        .flatMap(Client.request(convert(self.method), _, Headers.empty, inputToJsonData(self.inCodec.encoder)(input)))
//        .flatMap(_.getBodyAsString)
//        .map(decodeJson[Out](self.outCodec.decoder))
//        .absolve

//  private def inputToJsonData[A](encoder: JsonEncoder[A])(input: A) =
//    HttpData.fromString(encoder.encodeJson(input, None).toString, UTF_8)
