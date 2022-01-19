package server

import web.*
import zhttp.*
import zhttp.http.*
import zio.json.JsonDecoder

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
