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

  private[server] def convert(method: web.Method) = method match {
    case web.Method.GET  => zhttp.http.Method.GET
    case web.Method.POST => zhttp.http.Method.POST
  }

object v2:

  import web.v2.*

  // TODO invsetigar la posibilidad de usar map y contramap
  // TODO mejorar los tipos de respuesta de error
  def asZHTTP[R, In, Out](resolvedEndpoint: ResolvedEndpoint[R, In, Out]): HttpApp[R, Throwable] =
    val zMethod = syntax.convert(resolvedEndpoint.endpoint.method)
    resolvedEndpoint.endpoint match {
      case AnyUnitEndpoint(method, route) =>
        Http.collectZIO { case `zMethod` -> !! / `route` =>
          resolvedEndpoint.resolver(()).as(Response.ok)
        }
      case InCodecEndpoint(method, route, inCodec) =>
        Http.collectZIO { case request @ `zMethod` -> !! / `route` =>
          request.getBodyAsString
            .map(inCodec.decoder.decodeJson(_).left.map(new IllegalArgumentException(_)))
            .absolve
            .flatMap(resolvedEndpoint.resolver)
            .as(Response.ok)
        }
      case OutCodecEndpoint(method, route, outCodec) =>
        Http.collectZIO { case request @ `zMethod` -> !! / `route` =>
          resolvedEndpoint.resolver(()).map(outCodec.encoder.encodeJson(_, None).toString).map(Response.json)
        }
      case InOutCodecsEndpoint(method, route, inCodec, outCodec) =>
        Http.collectZIO { case request @ `zMethod` -> !! / `route` =>
          request.getBodyAsString
            .map(inCodec.decoder.decodeJson(_).left.map(new IllegalArgumentException(_)))
            .absolve
            .flatMap(resolvedEndpoint.resolver)
            .map(outCodec.encoder.encodeJson(_, None).toString)
            .map(Response.json)
        }
    }
