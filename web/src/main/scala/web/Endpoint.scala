package web

import zio.ZIO
import zio.json.*
import v4.*

enum Method:
  case GET, POST

object Endpoint:
  def get(route: String)                         = AnyUnitEndpoint(Method.GET, Route.init / route)
  def get[PathParams](route: Route[PathParams])  = ParamsAnyUnitEndpoint(Method.GET, route)
  def post(route: String)                        = AnyUnitEndpoint(Method.POST, Route.init / route)
  def post[PathParams](route: Route[PathParams]) = ParamsAnyUnitEndpoint(Method.POST, route)

object example:
  val echo: ParamsInOutEndpoint[(String, Int), Book, Book] =
    Endpoint
      .get(Route.init / "echo" / StringParam / IntParam)
      .in[Book]
      .out[Book]

  case class Book(name: String)

  given JsonCodec[Book] = DeriveJsonCodec.gen

object v4:

  val healthRoute: Route0              = Route.init / "health"
  val digitsRoute: Route1[String]      = Route.init / "echo" / StringParam
  val userPostsRoute: Route2[Int, Int] = Route.init / "users" / IntParam / "posts" / IntParam
  val userPostsCommentsRoute: Route3[String, Int, Int] =
    Start / "users" / StringParam / "posts" / IntParam / "comments" / IntParam
  val usersOrdersRoute: Route0 = Start / "users" / "orders"
  val a: Route2[Int, String]   = usersOrdersRoute / IntParam / StringParam

  println(healthRoute)
  println(digitsRoute)
  println(userPostsRoute)
  println(userPostsCommentsRoute)
  println(usersOrdersRoute)
  println(a)

  sealed trait Route[PathParams]

  object Route:
    val init: Start.type = Start

  object Start:
    def /(path: String): Route0   = Route0("/" + path)
    override def toString: String = "/"

  case class Route0(path: String) extends Route[Any]:
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

  sealed trait Param[A]:
    def fromStringUnsafe(input: String): A

  case object IntParam extends Param[Int]:
    override def fromStringUnsafe(input: String): Int = input.toInt

  case object StringParam extends Param[String]:
    override def fromStringUnsafe(input: String): String = input

  sealed trait ParamsEndpoint[PathParams, BodyIn, BodyOut]:
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

  sealed trait Endpoint[BodyIn, BodyOut]:
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

  sealed trait IncompleteEndpoint[In, Out]
  sealed trait IncompleteParamsEndpoint[P, In, Out]

  case class AnyUnitEndpoint(method: Method, route: Route0) extends Endpoint[Any, Unit]:
    def in[In](using codec: JsonCodec[In]): InEndpoint[In]      = InEndpoint[In](method, route, codec)
    def out[Out](using codec: JsonCodec[Out]): OutEndpoint[Out] = OutEndpoint[Out](method, route, codec)

  case class InEndpoint[In](method: Method, route: Route0, inCodec: JsonCodec[In]) extends Endpoint[In, Unit]:
    def out[Out](using codec: JsonCodec[Out]): InOutEndpoint[In, Out] =
      InOutEndpoint[In, Out](method, route, inCodec, codec)

  case class OutEndpoint[Out](method: Method, route: Route0, outCodec: JsonCodec[Out]) extends Endpoint[Any, Out]:
    def in[In](using codec: JsonCodec[In]): InOutEndpoint[In, Out] =
      InOutEndpoint[In, Out](method, route, codec, outCodec)

  case class InOutEndpoint[In, Out](method: Method, route: Route0, inCodec: JsonCodec[In], outCodec: JsonCodec[Out])
      extends Endpoint[In, Out]

  case class ParamsAnyUnitEndpoint[P](method: Method, route: Route[P]) extends ParamsEndpoint[P, Any, Unit]:
    def in[In](using codec: JsonCodec[In]): ParamsInEndpoint[P, In] = ParamsInEndpoint[P, In](method, route, codec)
    def out[Out](using codec: JsonCodec[Out]): ParamsOutEndpoint[P, Out] =
      ParamsOutEndpoint[P, Out](method, route, codec)

  case class ParamsInEndpoint[P, In](method: Method, route: Route[P], inCodec: JsonCodec[In])
      extends ParamsEndpoint[P, In, Unit]:
    def out[Out](using codec: JsonCodec[Out]): ParamsInOutEndpoint[P, In, Out] =
      ParamsInOutEndpoint[P, In, Out](method, route, inCodec, codec)

  case class ParamsOutEndpoint[P, Out](method: Method, route: Route[P], outCodec: JsonCodec[Out])
      extends ParamsEndpoint[P, Any, Out]:
    def in[In](using codec: JsonCodec[In]): ParamsInOutEndpoint[P, In, Out] =
      ParamsInOutEndpoint[P, In, Out](method, route, codec, outCodec)

  case class ParamsInOutEndpoint[P, In, Out](
    method: Method,
    route: Route[P],
    inCodec: JsonCodec[In],
    outCodec: JsonCodec[Out],
  ) extends ParamsEndpoint[P, In, Out]

  sealed trait EndpointWithResolver[-R, PathParams, BodyIn, BodyOut]

  case class ParamsEndpointWithResolver[-R, P, In, Out](
    endpoint: ParamsEndpoint[P, In, Out],
    resolver: (P, In) => ZIO[R, Throwable, Out],
  ) extends EndpointWithResolver[R, P, In, Out]

  case class NoParamsEndpointWithResolver[-R, In, Out](
    endpoint: Endpoint[In, Out],
    resolver: In => ZIO[R, Throwable, Out],
  ) extends EndpointWithResolver[R, Any, In, Out]
