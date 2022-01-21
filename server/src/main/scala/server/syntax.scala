package server

import endpoints.*
import endpoints.Route.*
import zhttp.*
import zhttp.http.*
import zio.ZIO
import zio.json.{JsonCodec, JsonDecoder}

import java.net.URLDecoder

object v4:

  import endpoints.*

  def asZHTTP[R](endpointWithResolver: EndpointWithResolver[R, _, _, _]): HttpApp[R, Throwable] =
    endpointWithResolver match {
      case NoParamsEndpointWithResolver(endpoint, resolver) =>
        val zMethod = convertMethod(endpoint.method)
        val zPath   = zhttp.http.Path(endpoint.route.path.split('/').toList)
        Http.collect { case request @ `zMethod` -> `zPath` => request }
          .collectZIO(extractBody(_, endpoint))
          .collectZIO(resolver(_))
          .map(encodeOut(endpoint))
      case ParamsEndpointWithResolver(endpoint, resolver) =>
        extractParams(endpoint.method, endpoint.route)
          .collectZIO((p, r) => extractBodyP(r, endpoint).map(p -> _))
          .collectZIO(resolver(_, _))
          .map(encodeOutP(endpoint))
    }

  def asZHTTP[R](errorMapper: Throwable => HttpError)(endpoint: EndpointWithResolver[R, _, _, _]): HttpApp[R, Nothing] =
    asZHTTP(endpoint).mapError(errorMapper).catchAll(Http.error)

  private def extractBody[In](request: Request, endpoint: NoParamsEndpoint[In, _]): ZIO[Any, Throwable, In] =
    endpoint match {
      case AnyUnitEndpoint(_, _) | OutEndpoint(_, _, _) => zio.ZIO.unit
      case InEndpoint(_, _, inCodec)                    => decodeBody(request, inCodec)
      case InOutEndpoint(_, _, inCodec, _)              => decodeBody(request, inCodec)
    }

  private def extractBodyP[In](request: Request, endpoint: ParamsEndpoint[_, In, _]): ZIO[Any, Throwable, In] =
    endpoint match {
      case ParamsAnyUnitEndpoint(_, _) | ParamsOutEndpoint(_, _, _) => zio.ZIO.unit
      case ParamsInEndpoint(_, _, inCodec)                          => decodeBody(request, inCodec)
      case ParamsInOutEndpoint(_, _, inCodec, _)                    => decodeBody(request, inCodec)
    }

  private def encodeOut[Out](endpoint: NoParamsEndpoint[_, Out])(out: Out): Response =
    endpoint match {
      case AnyUnitEndpoint(_, _) | InEndpoint(_, _, _) => Response(Status.NO_CONTENT)
      case OutEndpoint(_, _, outCodec)                 => encodeBody(out, outCodec)
      case InOutEndpoint(_, _, _, outCodec)            => encodeBody(out, outCodec)
    }

  private def encodeOutP[Out](endpoint: ParamsEndpoint[_, _, Out])(out: Out): Response =
    endpoint match {
      case ParamsAnyUnitEndpoint(_, _) | ParamsInEndpoint(_, _, _) => Response(Status.NO_CONTENT)
      case ParamsOutEndpoint(_, _, outCodec)                       => encodeBody(out, outCodec)
      case ParamsInOutEndpoint(_, _, _, outCodec)                  => encodeBody(out, outCodec)
    }

  private def decodeBody[In](request: Request, inCodec: JsonCodec[In]): ZIO[Any, Throwable, In] =
    request.getBodyAsString.map(inCodec.decodeJson(_).left.map(HttpError.BadRequest.apply)).absolve

  private def encodeBody[Out](out: Out, outCodec: JsonCodec[Out]): Response =
    Response.json(outCodec.encoder.encodeJson(out, None).toString)

  private def convertMethod(method: endpoints.Method) = method match {
    case endpoints.Method.GET  => zhttp.http.Method.GET
    case endpoints.Method.POST => zhttp.http.Method.POST
  }

  private def extractParams[PathParams](
    method: endpoints.Method,
    route: Route[PathParams],
  ): Http[Any, Nothing, Request, (PathParams, Request)] =
    val zMethod: zhttp.http.Method = convertMethod(method)
    route match {
      case Route0(path) =>
        val zPath = zhttp.http.Path(path.split('/').toList)
        Http.collect { case request @ `zMethod` -> `zPath` => () -> request }
      case Route1(prefix, param, path) if path.isBlank =>
        val prefixPath = zhttp.http.Path(prefix.path.split('/').toList)
        Http.collect { case request @ `zMethod` -> `prefixPath` / p =>
          param.fromStringUnsafe(URLDecoder.decode(p, "UTF-8")) -> request
        }
      case Route1(prefix, param, path) =>
        val prefixPath = zhttp.http.Path(prefix.path.split('/').toList)
        Http.collect { case request @ `zMethod` -> `prefixPath` / p / `path` => param.fromStringUnsafe(p) -> request }
      case Route2(prefix, param, path) if path.isBlank =>
        val prefixPath = zhttp.http.Path(prefix.prefix.path.split('/').toList)
        val aParam     = prefix.param
        ???
      case Route2(prefix, param, path) =>
        ???
      case Route3(prefix, param, path) if path.isBlank =>
        ???
      case Route3(prefix, param, path) =>
        ???
    }

//  private def decodeURL(input: String): Either
