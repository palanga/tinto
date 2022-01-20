package server

import web.*
import zhttp.*
import zhttp.http.*
import zio.ZIO
import zio.json.{JsonCodec, JsonDecoder}

object v4:

  import web.v4.*

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

  private def extractBody[In](request: Request, endpoint: Endpoint[In, _]): ZIO[Any, Throwable, In] =
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

  private def encodeOut[Out](endpoint: Endpoint[_, Out])(out: Out): Response =
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

  private def convertMethod(method: web.Method) = method match {
    case web.Method.GET  => zhttp.http.Method.GET
    case web.Method.POST => zhttp.http.Method.POST
  }

  private def extractParams[PathParams](
    method: web.Method,
    route: Route[PathParams],
  ): Http[Any, Nothing, Request, (PathParams, Request)] =
    val zMethod: zhttp.http.Method = convertMethod(method)
    route match {
      case Route0(path) =>
        val zPath = zhttp.http.Path(path.split('/').toList)
        Http.collect { case request @ `zMethod` -> `zPath` => () -> request }
      case Route1(prefix, param, path) if path.isBlank =>
        val prefixPath = zhttp.http.Path(prefix.path.split('/').toList)
        Http.collect { case request @ `zMethod` -> `prefixPath` / p => param.fromStringUnsafe(p) -> request }
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

object syntax:

  extension (self: Endpoint[_, _])
    def asZHTTP: HttpApp[Any, Throwable] =
      val method = convert(self.method)
      Http.collectZIO { case req @ `method` -> !! / self.route =>
        req.getBodyAsString
          .map(self.inCodec.decoder.decodeJson(_).left.map(new IllegalArgumentException(_)))
          .absolve
          .flatMap(self.resolver)
          .map(self.outCodec.encoder.encodeJson(_, None).toString)
          .map(Response.json)
      }

  private def convert(method: web.Method) = method match {
    case web.Method.GET  => zhttp.http.Method.GET
    case web.Method.POST => zhttp.http.Method.POST
  }

object v2:

  import web.v2.*

  // TODO invsetigar la posibilidad de usar map y contramap
  // TODO mejorar los tipos de respuesta de error
  def asZHTTP[R](endpointWithResolver: EndpointWithResolver[R, _, _]): HttpApp[R, Throwable] =
    endpointWithResolver match {
      case EndpointWithResolver(endpoint, resolver) =>
        val zMethod = convert(endpoint.method)
        Http.collectZIO { case request @ `zMethod` -> !! / endpoint.route =>
          endpoint match {
            case _: AnyUnitEndpoint =>
              resolver(())
                .as(Response(Status.NO_CONTENT))
            case endpoint: InCodecEndpoint[_] =>
              request.getBodyAsString
                .map(endpoint.inCodec.decoder.decodeJson(_).left.map(HttpError.BadRequest.apply))
                .absolve
                .flatMap(resolver)
                .as(Response(Status.NO_CONTENT))
            case endpoint: OutCodecEndpoint[_] =>
              resolver(())
                .map(endpoint.outCodec.encoder.encodeJson(_, None).toString)
                .map(Response.json)
            case endpoint: InOutCodecsEndpoint[_, _] =>
              request.getBodyAsString
                .map(endpoint.inCodec.decoder.decodeJson(_).left.map(HttpError.BadRequest.apply))
                .absolve
                .flatMap(resolver)
                .map(endpoint.outCodec.encoder.encodeJson(_, None).toString)
                .map(Response.json)
          }
        }
    }

  def asZHTTP[R](errorMapper: Throwable => HttpError)(endpoint: EndpointWithResolver[R, _, _]): HttpApp[R, Nothing] =
    asZHTTP(endpoint).mapError(errorMapper).catchAll(Http.error)

//  val a: HttpApp[Any, Throwable] = ???

//  val b: Http[Any, HttpError, Request, Response] = a.mapError {
//    case e: IllegalArgumentException => HttpError.BadRequest(e.getMessage)
//    case e                           => HttpError.InternalServerError(e.getMessage, Some(e.getCause))
//  }

  private def convert(method: web.Method) = method match {
    case web.Method.GET  => zhttp.http.Method.GET
    case web.Method.POST => zhttp.http.Method.POST
  }
