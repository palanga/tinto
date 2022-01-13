package api

import zhttp.http.HttpApp
import zio.ZIO

case class Endpoint[-In, +Out](
  route: String,
  resolver: In => ZIO[Any, Throwable, Out] = Endpoint.noop,
  method: Method = Method.GET,
):

  def map[B](f: Out => B): Endpoint[In, B] =
    val newResolver: In => ZIO[Any, Throwable, B] = this.resolver(_).map(f)
    this.copy(resolver = newResolver)

  def contramap[A](f: A => In): Endpoint[A, Out] =
    val newResolver: A => ZIO[Any, Throwable, Out] = input => this.resolver(f(input))
    this.copy(resolver = newResolver)

  def asZHTTP: HttpApp[Any, Throwable] =
    import zhttp.*
    import zhttp.http.*
    import zhttp.http.HttpApp
    import zhttp.service.Server

    HttpApp.collectM { case zhttp.http.Method.GET -> Root / route =>
      resolver(???).map(_.toString).map(Response.jsonString)
    }

case class UnResolvedEndpoint(
  route: String,
  method: Method = Method.GET,
):

  def resolveWith[In, Out](f: In => ZIO[Any, Throwable, Out]): Endpoint[In, Out] = Endpoint(this.route, f, this.method)

object Endpoint:

  def get(route: String) = UnResolvedEndpoint(route = route)

  private val noop: Any => ZIO[Any, Throwable, Unit] = _ => ZIO.unit

enum Method:
  case GET, POST

object example:

  val countDigits: Endpoint[Int, Int] =
    Endpoint
      .get("countDigits")
      .resolveWith(echoZ)
      .contramap[Int](_.toString)
      .map(_.size)

  val echo: Endpoint[String, String] =
    Endpoint
      .get("echo")
      .resolveWith(echoZ)

  def echoZ(input: String) = ZIO succeed input

object zhttpapi extends zio.App:

  import zhttp.*
  import zhttp.http.*
  import zhttp.http.HttpApp
  import zhttp.service.Server
  import zio.json.*

  val app: HttpApp[Any, Throwable] = HttpApp.collectM {
//    case zhttp.http.Method.GET -> Root / example.countDigits.route =>
//      example.countDigits.resolver(107).map(_.toJson).map(Response.jsonString)
    case zhttp.http.Method.GET -> Root / example.echo.route =>
      example.echo.resolver("hola").map(_.toJson).map(Response.jsonString)
  }

  val app2: HttpApp[Any, Throwable] = HttpApp.collectM {
    case zhttp.http.Method.GET -> Root / example.countDigits.route =>
      example.countDigits.resolver(107).map(_.toJson).map(Response.jsonString)
  }

  val apps = List(example.countDigits, example.echo).map(_.asZHTTP).reduceOption(_ +++ _).getOrElse(HttpApp.notFound)

  override def run(args: List[String]) = Server.start(8090, apps).exitCode
