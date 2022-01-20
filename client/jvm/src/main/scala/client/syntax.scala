package client

import web.v4.InOutEndpoint
import zhttp.http.{HTTP_CHARSET, Headers, HttpData, URL}
import zhttp.service.{ChannelFactory, Client, EventLoopGroup}
import zio.ZIO

object syntax:

  extension [In, Out](self: InOutEndpoint[In, Out])
    def fetch(input: In): ZIO[EventLoopGroup & ChannelFactory, Throwable, Out] =
      ZIO
        .fromEither(URL.fromString("http://localhost:8080/" + self.route))
        .flatMap(makeRequest(input))
        .flatMap(_.getBodyAsString)
        .map(self.outCodec.decoder.decodeJson(_).left.map(new Exception(_)))
        .absolve

    private def makeRequest(input: In)(url: URL) =
      Client.request(
        convert(self.method),
        url,
        Headers.empty,
        HttpData.fromString(self.inCodec.encoder.encodeJson(input, None).toString, HTTP_CHARSET),
      )

  private def convert(method: web.Method) = method match {
    case web.Method.GET  => zhttp.http.Method.GET
    case web.Method.POST => zhttp.http.Method.POST
  }
