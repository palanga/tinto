package api

import io.netty.buffer.ByteBuf
import io.netty.handler.codec.http.{HttpHeaderNames, HttpHeaderValues}
import io.netty.util.CharsetUtil.UTF_8
import zhttp.http.{Header, HttpApp, HttpData, Method, Request, Response, URL}
import zhttp.service.{ChannelFactory, Client, EventLoopGroup}
import zio.{Chunk, ZIO}
import zio.console.putStrLn
import zio.json.{DeriveJsonCodec, DeriveJsonDecoder, DeriveJsonEncoder, JsonCodec, JsonDecoder, JsonEncoder}

import scala.reflect.ClassTag

case class Endpoint[In: JsonCodec: ClassTag, Out: JsonCodec: ClassTag](
  route: String,
  resolver: In => ZIO[Any, Throwable, Out],
  method: Method,
):

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
    import scala.reflect.*
    s"$method /$route = ${classTag[In].runtimeClass.getCanonicalName} -> ${classTag[Out].runtimeClass.getCanonicalName}"

  def fetch(input: In): ZIO[EventLoopGroup & ChannelFactory, Throwable, Out] =
//    val headers = List(
//      Header.make(
//        HttpHeaderNames.CONTENT_TYPE,
//        HttpHeaderValues.APPLICATION_JSON, /*.concat("; ") + HttpHeaderValues.CHARSET + "=" + UTF_8*/
//      )
//    )

    val headers =
      Header.custom("Accept", "application/json, */*;q=0.5")
      ::
      Header.custom("Accept-Encoding", "gzip, deflate")
      ::
      Header.custom("Content-Type", "application/json")
        :: Nil

    ZIO
      .fromEither(URL.fromString("http://localhost:8080/" + route))
      .map(method -> _)
      .flatMap(endpoint => Client.request(Request(endpoint, content = inputToJsonData(input), headers = headers)))
      .map(response => asString(response.content).get)
      .map(summon[JsonCodec[Out]].decoder.decodeJson(_).toOption.get)

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

// TODO la nueva version de zhttp hace esto bien
def asString(content: HttpData[Any, Any]): Option[String] =
  content match {
    case HttpData.CompleteData(data) => Option(new String(data.toArray, UTF_8))
    case _                           => Option.empty
  }

def inputToJsonData[In: JsonCodec](input: In): HttpData[Any, Nothing] =
  import core.chaining.|>

  summon[JsonCodec[In]].encoder
    .encodeJson(input, None)
    .toString
    .getBytes(UTF_8) |> Chunk.fromArray |> HttpData.CompleteData.apply

object example:

  given JsonCodec[Book] = DeriveJsonCodec.gen

  val countDigits: Endpoint[Int, Int] =
    Endpoint
      .get("countDigits")
      .resolveWith(countDigitsZ)

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
    Server.port(8080) ++ // Setup port
//      Server.paranoidLeakDetection ++ // Paranoid leak detection (affects performance)
      Server.app(app)

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
    import core.chaining.|>
//    putStrLn(
//      JsonDecoder.string.decodeJson(asString(inputToJsonData("ñ")).get).toOption.get
//    )
//    *>
    server.make
      .use(_ =>
        putStrLn(docs)
          *> putStrLn("Server started on port 8080")
          *> fetching
          *> ZIO.never
      )
      .provideCustomLayer(
        zhttp.service.server.ServerChannelFactory.auto
          ++ zhttp.service.EventLoopGroup.auto(1)
          ++ zhttp.service.ChannelFactory.auto
      )
      .exitCode
