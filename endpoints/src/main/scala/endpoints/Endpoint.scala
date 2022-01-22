package endpoints

import endpoints.Route.*
import izumi.reflect.Tag
import zio.ZIO
import zio.json.*

enum Method:
  case GET, POST, DELETE, PATCH

object Endpoint:
  def get(route: String)                           = AnyUnitEndpoint(Method.GET, Route.init / route)
  def get[PathParams](route: Route[PathParams])    = ParamsAnyUnitEndpoint(Method.GET, route)
  def post(route: String)                          = AnyUnitEndpoint(Method.POST, Route.init / route)
  def post[PathParams](route: Route[PathParams])   = ParamsAnyUnitEndpoint(Method.POST, route)
  def delete(route: String)                        = AnyUnitEndpoint(Method.DELETE, Route.init / route)
  def delete[PathParams](route: Route[PathParams]) = ParamsAnyUnitEndpoint(Method.DELETE, route)
  def patch(route: String)                         = AnyUnitEndpoint(Method.PATCH, Route.init / route)
  def patch[PathParams](route: Route[PathParams])  = ParamsAnyUnitEndpoint(Method.PATCH, route)

object example:
  val echo: ParamsInOutEndpoint[(String, Int), Book, Book] =
    Endpoint
      .get(Route.init / "echo" / StringParam / IntParam)
      .in[Book]
      .out[Book]

  case class Book(name: String)

  given JsonCodec[Book] = DeriveJsonCodec.gen

sealed trait Endpoint[PathParams, BodyIn, BodyOut] extends HasDocs:
  val method: Method
  val route: Route[PathParams]

sealed trait ParamsEndpoint[PathParams, BodyIn, BodyOut] extends Endpoint[PathParams, BodyIn, BodyOut]:
  val method: Method
  val route: Route[PathParams]

  /**
   * Resolve this endpoint with a ZIO
   *
   * @param f
   *   a function that takes a tuple of path params and a request body and
   *   returns a ZIO result
   * @tparam R
   *   the type of dependencies
   * @return
   *   an endpoint with resolver object
   */
  def resolveWith[R](
    f: (PathParams, BodyIn) => ZIO[R, Throwable, BodyOut]
  ): EndpointWithResolver[R, PathParams, BodyIn, BodyOut] = ParamsEndpointWithResolver(this, f)

sealed trait NoParamsEndpoint[BodyIn, BodyOut] extends Endpoint[Any, BodyIn, BodyOut]:
  val method: Method
  val route: Route0

  /**
   * Resolve this endpoint with a ZIO
   *
   * @param f
   *   a function that takes and a request body and returns a ZIO result
   * @tparam R
   *   the type of dependencies
   * @return
   *   an endpoint with resolver object
   */
  def resolveWith[R](f: BodyIn => ZIO[R, Throwable, BodyOut]): EndpointWithResolver[R, Any, BodyIn, BodyOut] =
    NoParamsEndpointWithResolver(this, f)

case class AnyUnitEndpoint(method: Method, route: Route0) extends NoParamsEndpoint[Any, Unit]:
  def in[In](using codec: JsonCodec[In], classTag: Tag[In]): InEndpoint[In] = InEndpoint[In](method, route, codec)
  def out[Out](using codec: JsonCodec[Out], classTag: Tag[Out]): OutEndpoint[Out] =
    OutEndpoint[Out](method, route, codec)
  override def docs: String = s"$method $route"

case class InEndpoint[In: Tag](method: Method, route: Route0, inCodec: JsonCodec[In])
    extends NoParamsEndpoint[In, Unit]:
  def out[Out](using codec: JsonCodec[Out], tag: Tag[Out]): InOutEndpoint[In, Out] =
    InOutEndpoint[In, Out](method, route, inCodec, codec)
  override def docs: String = s"$method $route -> ${Tag[In].tag}"

case class OutEndpoint[Out: Tag](method: Method, route: Route0, outCodec: JsonCodec[Out])
    extends NoParamsEndpoint[Any, Out]:
  def in[In](using codec: JsonCodec[In], classTag: Tag[In]): InOutEndpoint[In, Out] =
    InOutEndpoint[In, Out](method, route, codec, outCodec)
  override def docs: String = s"$method $route -> ${Tag[Out].tag}"

case class InOutEndpoint[In: Tag, Out: Tag](
  method: Method,
  route: Route0,
  inCodec: JsonCodec[In],
  outCodec: JsonCodec[Out],
) extends NoParamsEndpoint[In, Out]:
  override def docs: String =
    s"$method $route -> ${Tag[In].tag} -> ${Tag[Out].tag}"

case class ParamsAnyUnitEndpoint[P](method: Method, route: Route[P]) extends ParamsEndpoint[P, Any, Unit]:
  def in[In](using codec: JsonCodec[In], classTag: Tag[In]): ParamsInEndpoint[P, In] =
    ParamsInEndpoint[P, In](method, route, codec)
  def out[Out](using codec: JsonCodec[Out], classTag: Tag[Out]): ParamsOutEndpoint[P, Out] =
    ParamsOutEndpoint[P, Out](method, route, codec)
  override def docs: String = s"$method $route"

case class ParamsInEndpoint[P, In: Tag](method: Method, route: Route[P], inCodec: JsonCodec[In])
    extends ParamsEndpoint[P, In, Unit]:
  def out[Out](using codec: JsonCodec[Out], classTag: Tag[Out]): ParamsInOutEndpoint[P, In, Out] =
    ParamsInOutEndpoint[P, In, Out](method, route, inCodec, codec)
  override def docs: String =
    s"$method $route -> ${Tag[In]}"

case class ParamsOutEndpoint[P, Out: Tag](method: Method, route: Route[P], outCodec: JsonCodec[Out])
    extends ParamsEndpoint[P, Any, Out]:
  def in[In](using codec: JsonCodec[In], classTag: Tag[In]): ParamsInOutEndpoint[P, In, Out] =
    ParamsInOutEndpoint[P, In, Out](method, route, codec, outCodec)
  override def docs: String =
    s"$method $route -> ${Tag[Out].tag}"

case class ParamsInOutEndpoint[P, In: Tag, Out: Tag](
  method: Method,
  route: Route[P],
  inCodec: JsonCodec[In],
  outCodec: JsonCodec[Out],
) extends ParamsEndpoint[P, In, Out]:
  override def docs: String =
    s"$method $route -> ${Tag[In]} -> ${Tag[Out].tag}"

sealed trait EndpointWithResolver[-R, PathParams, BodyIn, BodyOut]:
  val endpoint: Endpoint[PathParams, BodyIn, BodyOut]

case class ParamsEndpointWithResolver[-R, P, In, Out](
  endpoint: Endpoint[P, In, Out],
  resolver: (P, In) => ZIO[R, Throwable, Out],
) extends EndpointWithResolver[R, P, In, Out]

case class NoParamsEndpointWithResolver[-R, In, Out](
  endpoint: Endpoint[Any, In, Out],
  resolver: In => ZIO[R, Throwable, Out],
) extends EndpointWithResolver[R, Any, In, Out]

trait HasDocs:
  def docs: String

object v5:
  // TODO usar match types ?

  sealed trait Path[Head, +Tail]:
    def /(name: String): Path[Any, Path[Head, Tail]]             = Name(name, this)
    def /[P](paramType: ParamType[P]): Path[P, Path[Head, Tail]] = Param[P, Path[Head, Tail]](paramType, this)

    def toList: List[String | ParamType[_]] = (this match {
      case Start                  => List.empty
      case Name(name, tail)       => name :: tail.toList
      case Param(paramType, tail) => paramType :: tail.toList
    }).reverse

  object Path:
    val init: Start.type = Start

  case object Start extends Path[Nothing, Nothing]:
    override def /(name: String): Path[Any, Start.type]             = Name(name, Start)
    override def /[P](paramType: ParamType[P]): Path[P, Start.type] = Param[P, Start.type](paramType, Start)

  case class Name[B <: Path[_, _]](name: String, tail: B) extends Path[Any, B]

  case class Param[A, B <: Path[_, _]](paramType: ParamType[A], tail: B) extends Path[A, B]

  enum ParamType[A]:
    case StringParam extends ParamType[String]
    case IntParam extends ParamType[Int]

  import ParamType.*

  val r1: Start.type                                     = Path.init
  val r2: Path[Any, Start.type]                          = Path.init / "echo"
  val r3: Path[String, Path[Any, Start.type]]            = Path.init / "echo" / StringParam
  val r4: Path[Int, Path[String, Path[Any, Start.type]]] = Path.init / "echo" / StringParam / IntParam

object v6:
//  //  Endpoint[(String, Int), Book, String]
//  Endpoint.get
//    .path("echo")
//    .stringParam("The text to be echoed")
//    .path("delay")
//    .intParam("Delay in seconds")
//    .requestBody[Book]("The book from which the text has been taken")
//    .responseBody[String]("The echoed text")
//
//  //  Endpoint[(String, Int), Book, String]
//  GET / "echo" / Param.string("The text to be echoed") / "delay" / Param.int("Delay in seconds")
//    :: Body.custom[Book]("The book from which the text has been taken") -> Body.string("The echoed text")

  sealed trait Endpoint[Head, +Tail]

  case class MethodName(method: Method) extends Endpoint[MethodName, Nothing]:
    def path(name: String) = PathName(name, this)

  case class PathName(path: String, tail: MethodName | PathName | PathParam)
      extends Endpoint[PathName, MethodName | PathName | PathParam]:
    def stringParam(description: String) = PathParam(Param.string, this)

  case class PathParam(param: Param, tail: MethodName | PathName | PathParam)
      extends Endpoint[PathParam, MethodName | PathName | PathParam]

  case class RequestBody[A](codec: JsonCodec[A], tail: MethodName | PathName | PathParam)
      extends Endpoint[RequestBody[A], MethodName | PathName | PathParam]

  case class ResponseBody[A](codec: JsonCodec[A], tail: RequestBody[_])
      extends Endpoint[ResponseBody[A], RequestBody[_]]

  object Param:
    val string = StringParam
    val int    = StringParam

  sealed trait Param
  case object StringParam extends Param
  case object IntParam    extends Param
