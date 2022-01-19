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

  import Param.*

  val healthRoute: Route0              = Start / "health"
  val digitsRoute: Route1[String]      = Start / "echo" / StringParam
  val userPostsRoute: Route2[Int, Int] = Start / "users" / IntParam / "posts" / IntParam
  val userPostsCommentsRoute: Route3[String, Int, Int] =
    Start / "users" / StringParam / "posts" / IntParam / "comments" / IntParam

  val usersOrdersRoute: Route0 = Start / "users" / "orders"
  val a: Route2[Int, String]   = usersOrdersRoute / IntParam / StringParam

  sealed trait Route[-In]

  object Start:
    def /(path: String): Route0   = Route0("/" + path)
    override def toString: String = "/"

  case class Route0(path: String) extends Route[Unit]:
    def /(path: String): Route0          = copy(path = this.path + "/" + path)
    def /[A](param: Param[A]): Route1[A] = Route1(this, param)
    override def toString: String        = path

  case class Route1[A](prefix: Route0, param: Param[A], path: String = "") extends Route[A]:
    def /(path: String): Route1[A]          = copy(path = this.path + "/" + path)
    def /[B](param: Param[B]): Route2[A, B] = Route2(this, param)
    override def toString: String           = prefix.toString + "/" + param.toString + path

  case class Route2[A, B](prefix: Route1[A], param: Param[B], path: String = "") extends Route[(A, B)]:
    def /(path: String): Route2[A, B]          = copy(path = this.path + "/" + path)
    def /[C](param: Param[C]): Route3[A, B, C] = Route3(this, param)
    override def toString: String              = prefix.toString + "/" + param.toString + path

  case class Route3[A, B, C](prefix: Route2[A, B], param: Param[C], path: String = "") extends Route[(A, B, C)]:
    def /(path: String): Route3[A, B, C] = copy(path = this.path + "/" + path)
    override def toString: String        = prefix.toString + "/" + param.toString + path

  enum Param[A]:
    case IntParam extends Param[Int]
    case StringParam extends Param[String]

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
