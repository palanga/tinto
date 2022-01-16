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
