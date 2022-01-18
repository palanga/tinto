package web

import zio.ZIO
import zio.console.putStrLn
import zio.json.*

import scala.reflect.ClassTag
import scala.reflect.classTag

case class Endpoint[In, Out](
  route: String,
  resolver: In => ZIO[Any, Throwable, Out],
  method: Method,
  inCodec: JsonCodec[In],
  outCodec: JsonCodec[Out],
  inClassTag: ClassTag[In],
  outClassTag: ClassTag[Out],
):

  def doc: String =
    s"$method /$route = ${inClassTag.runtimeClass.getCanonicalName} -> ${outClassTag.runtimeClass.getCanonicalName}"

case class UnResolvedEndpoint(
  route: String,
  method: Method,
):

  def resolveWith[In: JsonCodec: ClassTag, Out: JsonCodec: ClassTag](
    f: In => ZIO[Any, Throwable, Out]
  ): Endpoint[In, Out] =
    Endpoint(this.route, f, this.method, summon[JsonCodec[In]], summon[JsonCodec[Out]], classTag[In], classTag[Out])

object Endpoint:

  // TODO get shouldn't have body
  def get(route: String)  = UnResolvedEndpoint(route, Method.GET)
  def post(route: String) = UnResolvedEndpoint(route, Method.POST)

enum Method:
  case GET, POST

object v2:

  sealed trait Endpoint[In, Out]:
    val method: Method
    val route: String
    def resolveWith[R](f: In => ZIO[R, Throwable, Out]): ResolvedEndpoint[R, In, Out] = ResolvedEndpoint(this, f)

  sealed trait IncompleteEndpoint[In, Out]

  object Endpoint:
    def get(route: String)  = AnyUnitEndpoint(Method.GET, route)
    def post(route: String) = AnyUnitEndpoint(Method.POST, route)

  case class AnyUnitEndpoint(method: Method, route: String) extends Endpoint[Any, Unit]:
    def in[In]: InEndpoint[In]     = InEndpoint[In](method, route)
    def out[Out]: OutEndpoint[Out] = OutEndpoint[Out](method, route)

  case class InEndpoint[In](method: Method, route: String) extends IncompleteEndpoint[In, Unit]:
    def withInCodec(using inCodec: JsonCodec[In]): InCodecEndpoint[In] = InCodecEndpoint(method, route, inCodec)
    def out[Out]: InOutEndpoint[In, Out]                               = InOutEndpoint[In, Out](method, route)

  case class OutEndpoint[Out](method: Method, route: String) extends IncompleteEndpoint[Any, Out]:
    def withOutCodec(using outCodec: JsonCodec[Out]): OutCodecEndpoint[Out] =
      OutCodecEndpoint(method, route, outCodec)
    def in[In]: InOutEndpoint[In, Out] = InOutEndpoint[In, Out](method, route)

  case class InOutEndpoint[In, Out](method: Method, route: String) extends IncompleteEndpoint[In, Out]:
    def withInCodec(using inCodec: JsonCodec[In]): InCodecOutEndpoint[In, Out] =
      InCodecOutEndpoint(method, route, inCodec)
    def withOutCodec(using outCodec: JsonCodec[Out]): InOutCodecEndpoint[In, Out] =
      InOutCodecEndpoint(method, route, outCodec)

  case class InCodecEndpoint[In](method: Method, route: String, inCodec: JsonCodec[In]) extends Endpoint[In, Unit]:
    def out[Out]: InCodecOutEndpoint[In, Out] = InCodecOutEndpoint[In, Out](method, route, inCodec)

  case class OutCodecEndpoint[Out](method: Method, route: String, outCodec: JsonCodec[Out]) extends Endpoint[Any, Out]:
    def in[In]: InOutCodecEndpoint[In, Out] = InOutCodecEndpoint[In, Out](method, route, outCodec)

  case class InCodecOutEndpoint[In, Out](method: Method, route: String, inCodec: JsonCodec[In])
      extends IncompleteEndpoint[In, Out]:
    def withOutCodec(using outCodec: JsonCodec[Out]): InOutCodecsEndpoint[In, Out] =
      InOutCodecsEndpoint(method, route, inCodec, outCodec)

  case class InOutCodecEndpoint[In, Out](method: Method, route: String, outCodec: JsonCodec[Out])
      extends IncompleteEndpoint[In, Out]:
    def withInCodec(using inCodec: JsonCodec[In]): InOutCodecsEndpoint[In, Out] =
      InOutCodecsEndpoint(method, route, inCodec, outCodec)

  case class InOutCodecsEndpoint[In, Out](
    method: Method,
    route: String,
    inCodec: JsonCodec[In],
    outCodec: JsonCodec[Out],
  ) extends Endpoint[In, Out]

  case class ResolvedEndpoint[-R, In, Out](endpoint: Endpoint[In, Out], resolver: In => ZIO[R, Throwable, Out])

  case class DocumentedEndpoint[-R, In, Out](endpoint: Endpoint[In, Out], docs: String)

  case class DocumentedResolvedEndpoint[-R, In, Out](
    endpoint: Endpoint[In, Out],
    resolver: In => ZIO[R, Throwable, Out],
    docs: String,
  )

  object example:

    Endpoint
      .get("")
      .in[String]
      .out[Int]
      .withInCodec
      .withOutCodec
