package examples

import web.Endpoint
import zhttp.http.CORSConfig
import zio.ZIO
import zio.console.putStrLn
import zio.json.{DeriveJsonCodec, JsonCodec}

object example:

  given JsonCodec[Book] = DeriveJsonCodec.gen

  val countDigits =
    Endpoint
      .post("countDigits")
      .resolveWith(countDigitsZ)

  val echo =
    Endpoint
      .post("echo")
      .resolveWith(echoZ)

  val books =
    Endpoint
      .post("books")
      .resolveWith(bookZ)

  case class Book(name: String)
  case class Views(amount: Int)

  import zio.duration.*
  def echoZ(input: String)     = ZIO succeed input delay 6.seconds provideLayer zio.clock.Clock.live
  def countDigitsZ(input: Int) = ZIO succeed input.toString.length
  def bookZ(input: Book)       = ZIO succeed input

object zhttpapi extends zio.App:

  import zhttp.service.Server
  import zhttp.http.Middleware.cors
  import server.syntax.asZHTTP
  import client.syntax.fetch

  val endpoints =
    List(
      example.echo,
      example.countDigits,
      example.books,
    )

//  val docs = endpoints.map(_.doc).mkString("\n")
  val app = endpoints.map(_.asZHTTP).reduce(_ ++ _)

  private val port = 8080

  private val appServer = Server.port(port) ++ Server.app(app @@ cors(CORSConfig(true)))
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
    appServer.make
      .use(_ =>
        putStrLn("docs TODO")
          *> putStrLn(s"Server started on port $port")
//          *> fetching
          *> ZIO.never
      )
      .provideCustomLayer(
        zhttp.service.server.ServerChannelFactory.auto
          ++ zhttp.service.EventLoopGroup.auto(1)
          ++ zhttp.service.ChannelFactory.auto
      )
      .exitCode
