package server

import web.{Endpoint, decodeJson}
import zhttp.*
import zhttp.http.*

object syntax:

  extension (self: Endpoint[_, _])
    def asZHTTP: HttpApp[Any, Throwable] =
      val method = convert(self.method)
      Http.collectZIO { case req @ method -> Root / self.route => // TODO machea todos los metodos
        req.getBodyAsString
          .map(decodeJson(self.inCodec.decoder))
          .absolve
          .flatMap(self.resolver)
          .map(self.outCodec.encoder.encodeJson(_, None).toString)
          .map(Response.json)
      }

  private def convert(method: web.Method) = method match {
    case web.Method.GET  => zhttp.http.Method.GET
    case web.Method.POST => zhttp.http.Method.POST
  }
