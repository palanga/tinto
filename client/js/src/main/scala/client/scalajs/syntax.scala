package client.scalajs

import sttp.capabilities
import sttp.client3.*
import sttp.model.{Method, Uri}
import endpoints.*
import endpoints.Route.*
import zio.json.JsonEncoder
import zio.{IO, ZIO, ZManaged}

import scala.concurrent.Future

object client:

  def fetch[PathParams, BodyIn, BodyOut](endpoint: ParamsEndpoint[PathParams, BodyIn, BodyOut])(
    input: (PathParams, BodyIn)
  ): ZIO[Any, Throwable, BodyOut] =
    val path        = buildPath(endpoint.route, input._1)
    val uri         = Uri.parse(s"http://localhost:8080$path").left.map(new Exception(_))
    val method      = convert(endpoint.method)
    val requestBody = encodeRequestBodyP(endpoint, input._2)
    ZIO
      .fromEither(uri)
      .flatMap(makeRequest(method, requestBody))
      .map(decodeResponseBodyP(endpoint))
      .absolve

  def fetch[BodyIn, BodyOut](endpoint: NoParamsEndpoint[BodyIn, BodyOut])(input: BodyIn): ZIO[Any, Throwable, BodyOut] =
    val uri         = Uri.parse(s"http://localhost:8080${endpoint.route.path}").left.map(new Exception(_))
    val method      = convert(endpoint.method)
    val requestBody = encodeRequestBody(endpoint, input)
    ZIO
      .fromEither(uri)
      .flatMap(makeRequest(method, requestBody))
      .map(decodeResponseBody(endpoint))
      .absolve

  private def buildPath[P](route: Route[P], pathParams: Any): String =
    (route, pathParams) match {
      case Route0(path) -> ()                   => path
      case Route1(prefix, _, path) -> a         => buildPath(prefix, ()) + "/" + a.toString + path
      case Route2(prefix, _, path) -> (a, b)    => buildPath(prefix, a) + "/" + b.toString + path
      case Route3(prefix, _, path) -> (a, b, c) => buildPath(prefix, (a, b)) + "/" + c.toString + path
      case _                                    => ""
    }

  private def encodeRequestBody[In](endpoint: NoParamsEndpoint[In, _], input: In): String = endpoint match {
    case AnyUnitEndpoint(_, _) | OutEndpoint(_, _, _) => ""
    case InEndpoint(_, _, inCodec)                    => inCodec.encodeJson(input, None).toString
    case InOutEndpoint(_, _, inCodec, _)              => inCodec.encodeJson(input, None).toString
  }

  private def encodeRequestBodyP[In](endpoint: ParamsEndpoint[_, In, _], input: In): String = endpoint match {
    case ParamsAnyUnitEndpoint(_, _) | ParamsOutEndpoint(_, _, _) => ""
    case ParamsInEndpoint(_, _, inCodec)                          => inCodec.encodeJson(input, None).toString
    case ParamsInOutEndpoint(_, _, inCodec, _)                    => inCodec.encodeJson(input, None).toString
  }

  private def decodeResponseBody[Out](endpoint: NoParamsEndpoint[_, Out])(
    response: Response[Either[String, String]]
  ): Either[Exception, Out] = endpoint match {
    case AnyUnitEndpoint(_, _) | InEndpoint(_, _, _) => Right(())
    case OutEndpoint(_, _, outCodec)                 => response.body.flatMap(outCodec.decodeJson).left.map(Exception(_))
    case InOutEndpoint(_, _, _, outCodec)            => response.body.flatMap(outCodec.decodeJson).left.map(Exception(_))
  }

  private def decodeResponseBodyP[Out](endpoint: ParamsEndpoint[_, _, Out])(
    response: Response[Either[String, String]]
  ): Either[Exception, Out] = endpoint match {
    case ParamsAnyUnitEndpoint(_, _) | ParamsInEndpoint(_, _, _) => Right(())
    case ParamsOutEndpoint(_, _, outCodec)                       => response.body.flatMap(outCodec.decodeJson).left.map(Exception(_))
    case ParamsInOutEndpoint(_, _, _, outCodec)                  => response.body.flatMap(outCodec.decodeJson).left.map(Exception(_))
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

  private def convert(method: endpoints.Method) = method match {
    case endpoints.Method.GET    => sttp.model.Method.GET
    case endpoints.Method.POST   => sttp.model.Method.POST
    case endpoints.Method.DELETE => sttp.model.Method.DELETE
    case endpoints.Method.PATCH  => sttp.model.Method.PATCH
  }
