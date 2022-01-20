package client.scalajs

import sttp.capabilities
import sttp.client3.*
import sttp.model.Method.isIdempotent
import sttp.model.{Method, Uri}
import web.v4.InOutEndpoint
import zio.json.JsonEncoder
import zio.{IO, ZIO, ZManaged}

import scala.concurrent.Future

object client:
  import web.v4.*

  def fetch[In, Out](endpoint: Endpoint[In, Out])(input: In): ZIO[Any, Throwable, Out] =
    val uri         = Uri.parse(s"http://localhost:8080/${endpoint.route.path}").left.map(new Exception(_))
    val method      = convert(endpoint.method)
    val requestBody = encodeRequestBody(endpoint, input)
    ZIO
      .fromEither(uri)
      .flatMap(makeRequest(method, requestBody))
      .map(decodeResponseBody(endpoint))
      .absolve

  private def encodeRequestBody[In](endpoint: Endpoint[In, _], input: In): String = endpoint match {
    case AnyUnitEndpoint(_, _) | OutEndpoint(_, _, _) => ""
    case InEndpoint(_, _, inCodec)                    => inCodec.encodeJson(input, None).toString
    case InOutEndpoint(_, _, inCodec, _)              => inCodec.encodeJson(input, None).toString
  }

  private def decodeResponseBody[In, Out](endpoint: Endpoint[In, Out])(
    response: Response[Either[String, String]]
  ): Either[Exception, Out] = endpoint match {
    case AnyUnitEndpoint(_, _) | InEndpoint(_, _, _) => Right(())
    case OutEndpoint(_, _, outCodec)                 => response.body.flatMap(outCodec.decodeJson).left.map(Exception(_))
    case InOutEndpoint(_, _, _, outCodec)            => response.body.flatMap(outCodec.decodeJson).left.map(Exception(_))
  }

  private def makeRequest(method: Method, body: String)(
    uri: Uri
  ): ZIO[Any, Throwable, Response[Either[String, String]]] =
    makeBackend.use(backend =>
      ZIO fromFuture { _ =>
        if (Method.isSafe(method) || body.isBlank)
          sttp.client3.basicRequest.method(method, uri).send(backend)
        else
          sttp.client3.basicRequest.method(method, uri).body(body).send(backend)
      }
    )

  private def makeBackend: ZManaged[Any, Nothing, SttpBackend[Future, capabilities.WebSockets]] =
    ZIO.succeed(FetchBackend()).toManaged(backend => ZIO.fromFuture(_ => backend.close()).ignore)

  private def convert(method: web.Method) = method match {
    case web.Method.GET  => sttp.model.Method.GET
    case web.Method.POST => sttp.model.Method.POST
  }

object syntax:

  extension [In, Out](self: InOutEndpoint[In, Out])
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
