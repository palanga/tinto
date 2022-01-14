package web

import io.netty.util.CharsetUtil.UTF_8
import zhttp.*
import zhttp.http.*
import zhttp.service.*
import zio.ZIO
import zio.console.putStrLn
import zio.json.*

import scala.reflect.ClassTag

case class Endpoint[In: JsonCodec: ClassTag, Out: JsonCodec: ClassTag](
  route: String,
  resolver: In => ZIO[Any, Throwable, Out],
  method: Method,
):

  def asZHTTP: HttpApp[Any, Throwable] =
    Http.collectZIO { case req @ `method` -> Root / `route` =>
      req.getBodyAsString
        .map(decodeJson[In])
        .absolve
        .flatMap(resolver)
        .map(summon[JsonEncoder[Out]].encodeJson(_, None).toString)
        .map(Response.json)
    }

  def doc: String =
    import scala.reflect.*
    s"$method /$route = ${classTag[In].runtimeClass.getCanonicalName} -> ${classTag[Out].runtimeClass.getCanonicalName}"

  def fetch(input: In): ZIO[EventLoopGroup & ChannelFactory, Throwable, Out] =
    ZIO
      .fromEither(URL.fromString("http://localhost:8080/" + route))
      .flatMap(Client.request(method, _, Headers.empty, inputToJsonData(input)))
      .flatMap(_.getBodyAsString)
      .map(decodeJson[Out])
      .absolve

  private def decodeJson[A: JsonDecoder](input: String) =
    summon[JsonDecoder[A]].decodeJson(input).left.map(new IllegalArgumentException(_))

  private def inputToJsonData(input: In) =
    HttpData.fromString(summon[JsonCodec[In]].encoder.encodeJson(input, None).toString, UTF_8)

case class UnResolvedEndpoint(
  route: String,
  method: Method,
):

  def resolveWith[In: JsonCodec: ClassTag, Out: JsonCodec: ClassTag](
    f: In => ZIO[Any, Throwable, Out]
  ): Endpoint[In, Out] =
    Endpoint(this.route, f, this.method)

object Endpoint:

  def get(route: String) = UnResolvedEndpoint(route, Method.GET)

  private val noop: Any => ZIO[Any, Throwable, Unit] = _ => ZIO.unit

object example:

  given JsonCodec[Book] = DeriveJsonCodec.gen

  val countDigits =
    Endpoint
      .get("countDigits")
      .resolveWith(countDigitsZ)

  val echo =
    Endpoint
      .get("echo")
      .resolveWith(echoZ)

  val books =
    Endpoint
      .get("books")
      .resolveWith(bookZ)

  case class Book(name: String)
  case class Views(amount: Int)

  def echoZ(input: String)     = ZIO succeed input
  def countDigitsZ(input: Int) = ZIO succeed input.toString.length
  def bookZ(input: Book)       = ZIO succeed input

object zhttpapi extends zio.App:

  import zhttp.service.Server

  val endpoints =
    List(
      example.echo,
      example.countDigits,
      example.books,
    )

  val docs = endpoints.map(_.doc).mkString("\n")
  val app  = endpoints.map(_.asZHTTP).reduce(_ ++ _)

  private val port = 8080

  private val server = Server.port(port) ++ Server.app(app)
//      ++ Server.paranoidLeakDetection // Paranoid leak detection (affects performance)

  val fetching =
    example.books
      .fetch(example.Book("ñ"))
      .map(_.toString)
      .flatMap(putStrLn(_))
    <&>
    example.countDigits
      .fetch(107)
      .map(_.toString)
      .flatMap(putStrLn(_))
    <&>
    example.echo
      .fetch("🧉")
      .map(_.toString)
      .flatMap(putStrLn(_))

  override def run(args: List[String]) =
    server.make
      .use(_ =>
        putStrLn(docs)
          *> putStrLn(s"Server started on port $port")
          *> fetching
          *> ZIO.never
      )
      .provideCustomLayer(
        zhttp.service.server.ServerChannelFactory.auto
          ++ zhttp.service.EventLoopGroup.auto(1)
          ++ zhttp.service.ChannelFactory.auto
      )
      .exitCode
