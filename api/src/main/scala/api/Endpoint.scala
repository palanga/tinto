package api

import zhttp.http.{HttpApp, Method}
import zio.ZIO
import zio.console.putStrLn
import zio.json.{DeriveJsonDecoder, JsonDecoder, JsonEncoder, JsonCodec, DeriveJsonCodec, DeriveJsonEncoder}

import scala.reflect.ClassTag

case class Endpoint[In: JsonDecoder: ClassTag, Out: JsonEncoder: ClassTag](
  route: String,
  resolver: In => ZIO[Any, Throwable, Out],
  method: Method,
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

    HttpApp.collectM { case req @ `method` -> Root / `route` =>
      ZIO
        .fromEither(
          req.getBodyAsString
            .toRight("No body")
            .flatMap(summon[JsonDecoder[In]].decodeJson)
            .left
            .map(e => new IllegalArgumentException(e))
        )
        .flatMap(resolver)
        .map(summon[JsonEncoder[Out]].encodeJson(_, None).toString)
        .map(Response.jsonString)
    }

  def doc =
    import scala.reflect._
    s"$method /$route = ${classTag[In].runtimeClass.getCanonicalName} -> ${classTag[Out].runtimeClass.getCanonicalName}"

case class UnResolvedEndpoint(
  route: String,
  method: Method = Method.GET,
):

  def resolveWith[In: JsonDecoder: ClassTag, Out: JsonEncoder: ClassTag](
    f: In => ZIO[Any, Throwable, Out]
  ): Endpoint[In, Out] =
    Endpoint(this.route, f, this.method)

object Endpoint:

  def get(route: String) = UnResolvedEndpoint(route = route)

  private val noop: Any => ZIO[Any, Throwable, Unit] = _ => ZIO.unit

object example:

  given JsonCodec[Book] = DeriveJsonCodec.gen

  val countDigits: Endpoint[Int, Int] =
    Endpoint
      .get("countDigits")
      .resolveWith(countDigitsZ)
//      .contramap[Int](_.toString)
//      .map(_.size)

  val echo: Endpoint[String, String] =
    Endpoint
      .get("echo")
      .resolveWith(echoZ)

  val books: Endpoint[Book, Book] =
    Endpoint
      .get("books")
      .resolveWith(bookZ)

  case class Book(name: String)
  case class Views(amount: Int)

  def echoZ(input: String)     = ZIO succeed input
  def countDigitsZ(input: Int) = ZIO succeed input.toString.length
  def bookZ(input: Book)       = ZIO succeed input

object zhttpapi extends zio.App:

  import zhttp.http.HttpApp
  import zhttp.service.Server

  val endpoints =
    List(
      example.echo,
      example.countDigits,
      example.books,
    )

  val docs = endpoints.map(_.doc).mkString("\n")
  val app  = endpoints.map(_.asZHTTP).reduce(_ +++ _)

  private val server =
    Server.port(8090) ++ // Setup port
//      Server.paranoidLeakDetection ++ // Paranoid leak detection (affects performance)
      Server.app(app)

  override def run(args: List[String]) =
    server.make
      .use(_ => putStrLn(docs) *> putStrLn("Server started on port 8090") *> zio.ZIO.never)
      .provideCustomLayer(zhttp.service.server.ServerChannelFactory.auto ++ zhttp.service.EventLoopGroup.auto(1))
      .exitCode
//    Server.start(8090, apps).exitCode
