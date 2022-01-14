package api

import zhttp.http.HttpApp
import zio.ZIO
import zio.json.JsonDecoder

case class Endpoint[In, Out](
  route: String,
  resolver: In => ZIO[Any, Throwable, Out],
  method: Method = Method.GET,
  decoder: JsonDecoder[In],
):

//  def map[B](f: Out => B): Endpoint[In, B] =
//    val newResolver: In => ZIO[Any, Throwable, B] = this.resolver(_).map(f)
//    this.copy(resolver = newResolver)
//
//  def contramap[A](f: A => In): Endpoint[A, Out] =
//    val newResolver: A => ZIO[Any, Throwable, Out] = input => this.resolver(f(input))
//    this.copy(resolver = newResolver)

  def asZHTTP: HttpApp[Any, Throwable] =
    import zhttp.*
    import zhttp.http.*

    HttpApp.collectM { case req @ zhttp.http.Method.GET -> Root / `route` =>
      ZIO
        .fromEither(
          req.getBodyAsString
            .toRight("No body")
            .flatMap(decoder.decodeJson)
            .left
            .map(e => new IllegalArgumentException(e))
        )
        .flatMap(resolver)
        .map(_.toString)
        .map(Response.text)
    }

case class UnResolvedEndpoint(
  route: String,
  method: Method = Method.GET,
):

  def resolveWith[In, Out](f: In => ZIO[Any, Throwable, Out], decoder: JsonDecoder[In]): Endpoint[In, Out] =
    Endpoint(this.route, f, this.method, decoder)

object Endpoint:

  def get(route: String) = UnResolvedEndpoint(route = route)

  private val noop: Any => ZIO[Any, Throwable, Unit] = _ => ZIO.unit

enum Method:
  case GET, POST

object example:

  val countDigits: Endpoint[Int, Int] =
    Endpoint
      .get("countDigits")
      .resolveWith(countDigitsZ, zio.json.JsonDecoder.int)
//      .contramap[Int](_.toString)
//      .map(_.size)

  val echo: Endpoint[String, String] =
    Endpoint
      .get("echo")
      .resolveWith(echoZ, zio.json.JsonDecoder.string)

  case class Book(name: String)
  case class Views(amount: Int)

  def echoZ(input: String)     = ZIO succeed input
  def countDigitsZ(input: Int) = ZIO succeed input.toString.length

object zhttpapi extends zio.App:

  import zhttp.http.HttpApp
  import zhttp.service.Server

//  val zhttpEndpoints =
//    List(
//      example.echo,
//      example.countDigits,
//    ).map(_.asZHTTP).reduceOption(_ +++ _).getOrElse(HttpApp.notFound)

  val documentation =
    List(
      example.echo,
      example.countDigits,
    ).map(_.doc)

  val apps =
    List(
      example.countDigits,
      example.echo,
    ).map(_.asZHTTP).reduceOption(_ +++ _).getOrElse(HttpApp.notFound) +++ HttpApp.notFound

  private val server =
    Server.port(8090) ++ // Setup port
//      Server.paranoidLeakDetection ++ // Paranoid leak detection (affects performance)
      Server.app(apps)

  override def run(args: List[String]) =
    server.make
      .use(_ => zio.console.putStrLn(s"Server started on port 8090") *> zio.ZIO.never)
      .provideCustomLayer(zhttp.service.server.ServerChannelFactory.auto ++ zhttp.service.EventLoopGroup.auto(1))
      .exitCode
//    Server.start(8090, apps).exitCode
