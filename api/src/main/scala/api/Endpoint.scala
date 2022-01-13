package api

import zhttp.http.HttpApp
import zio.ZIO
import zio.json.JsonDecoder

case class Endpoint[In, Out](
  route: String,
  resolver: In => ZIO[Any, Throwable, Out] = Endpoint.noop,
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

    val dea: Http[Any, Throwable, In, Out] = Http.collectM[In] { case a => resolver(a) }

    val dea2: Http[Any, Throwable, Request, Out] = dea.contramap[Request] {
      case req @ zhttp.http.Method.GET -> Root / route =>
        val dea3: In = req.getBodyAsString
          .toRight("No body")
          .flatMap(decoder.decodeJson)
          .fold(e => throw new IllegalArgumentException(e), identity)

        dea3

    }

    val dea3: Http[Any, Throwable, Request, UResponse] = dea2.map(_.toString).map(Response.jsonString)

    dea3
//    HttpApp.collectM { case zhttp.http.Method.GET -> Root / route =>
//      resolver(???).map(_.toString).map(Response.jsonString)
//    }

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

  val bookDecoder: JsonDecoder[Book]   = zio.json.DeriveJsonDecoder.gen
  val viewsDecoder: JsonDecoder[Views] = zio.json.DeriveJsonDecoder.gen

  val countDigits: Endpoint[Views, Int] =
    Endpoint
      .get("countDigits")
      .resolveWith(countDigitsZ, viewsDecoder)
//      .contramap[Int](_.toString)
//      .map(_.size)

  val echo: Endpoint[Book, String] =
    Endpoint
      .get("echo")
      .resolveWith(echoZ, bookDecoder)

  case class Book(name: String)
  case class Views(amount: Int)

  def echoZ(input: Book)         = ZIO succeed input.name
  def countDigitsZ(input: Views) = ZIO succeed input.amount

object zhttpapi extends zio.App:

//  import zhttp.*
//  import zhttp.http.*
  import zhttp.http.HttpApp
  import zhttp.service.Server
//  import zio.json.*

//  val app: HttpApp[Any, Throwable] = HttpApp.collectM {
////    case zhttp.http.Method.GET -> Root / example.countDigits.route =>
////      example.countDigits.resolver(107).map(_.toJson).map(Response.jsonString)
//    case zhttp.http.Method.GET -> Root / example.echo.route =>
//      example.echo.resolver("hola").map(_.toJson).map(Response.jsonString)
//  }
//
//  val app2: HttpApp[Any, Throwable] = HttpApp.collectM {
//    case zhttp.http.Method.GET -> Root / example.countDigits.route =>
//      example.countDigits.resolver(107).map(_.toJson).map(Response.jsonString)
//  }

  val apps =
    List(
      example.echo,
      example.countDigits,
    ).map(_.asZHTTP).reduceOption(_ +++ _).getOrElse(HttpApp.notFound)

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
