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
    def resolveWith[R](f: In => ZIO[R, Throwable, Out]): EndpointWithResolver[R, In, Out] =
      EndpointWithResolver(this, f)

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

  case class EndpointWithResolver[-R, In, Out](endpoint: Endpoint[In, Out], resolver: In => ZIO[R, Throwable, Out])

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

object v3 extends App:

  import Param.*

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

  sealed trait Route[-In]

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

  enum Param[A]:
    case IntParam extends Param[Int]
    case StringParam extends Param[String]

//    override def toString: String = this match {
//      case IntParam    => "<<Int>>"
//      case StringParam => "<<String>>"
//    }

  sealed trait Endpoint[PathParams, In, Out]:
    val method: Method
    val route: Route[PathParams]
//    def resolveWith[R](f: ((PathParams, In)) => ZIO[R, Throwable, Out]): EndpointWithResolver[R, In, Out] =
//      EndpointWithResolver(this, f)

  sealed trait IncompleteEndpoint[PathParams, In, Out]

  object Endpoint:
    def get(route: String)      = AnyUnitEndpoint(Method.GET, Route.init / route)
    def get[A](route: Route[A]) = AnyUnitEndpoint(Method.GET, route)
//    def post(route: String)      = AnyUnitEndpoint(Method.POST, route)
//    def post[A](route: Route[A]) = WithPathParams[A](Method.POST, route)

  case class AnyUnitEndpoint[PathParams](method: Method, route: Route[PathParams])
      extends Endpoint[PathParams, Any, Unit]:
    def in[In]: InEndpoint[PathParams, In] = InEndpoint[PathParams, In](method, route)
//    def out[Out]: OutEndpoint[PathParams, Out] = OutEndpoint[PathParams, Out](method, route)

//  case class WithPathParams[A](method: Method, route: Route[A]) extends Endpoint[A, Any, Unit]

  case class InEndpoint[PathParams, In](method: Method, route: Route[PathParams])
      extends IncompleteEndpoint[PathParams, In, Unit]:
    def withInCodec(using inCodec: JsonCodec[In]): InCodecEndpoint[PathParams, In] =
      InCodecEndpoint(method, route, inCodec)
//    def out[Out] = InOutEndpoint[PathParams, In, Out](method, route)

//  case class OutEndpoint[PathParams, Out](method: Method, route: Route[PathParams])
//      extends IncompleteEndpoint[PathPArams, Any, Out]:
//    def withOutCodec(using outCodec: JsonCodec[Out]): OutCodecEndpoint[Out] =
//      OutCodecEndpoint(method, route, outCodec)
//    def in[In]: InOutEndpoint[In, Out] = InOutEndpoint[In, Out](method, route)
//
//  case class InOutEndpoint[In, Out](method: Method, route: String) extends IncompleteEndpoint[In, Out]:
//    def withInCodec(using inCodec: JsonCodec[In]): InCodecOutEndpoint[In, Out] =
//      InCodecOutEndpoint(method, route, inCodec)
//    def withOutCodec(using outCodec: JsonCodec[Out]): InOutCodecEndpoint[In, Out] =
//      InOutCodecEndpoint(method, route, outCodec)
//
  case class InCodecEndpoint[PathParams, In](method: Method, route: Route[PathParams], inCodec: JsonCodec[In])
      extends Endpoint[PathParams, In, Unit]
//    def out[Out]: InCodecOutEndpoint[In, Out] = InCodecOutEndpoint[In, Out](method, route, inCodec)
//
//  case class OutCodecEndpoint[Out](method: Method, route: String, outCodec: JsonCodec[Out]) extends Endpoint[Any, Out]:
//    def in[In]: InOutCodecEndpoint[In, Out] = InOutCodecEndpoint[In, Out](method, route, outCodec)
//
//  case class InCodecOutEndpoint[In, Out](method: Method, route: String, inCodec: JsonCodec[In])
//      extends IncompleteEndpoint[In, Out]:
//    def withOutCodec(using outCodec: JsonCodec[Out]): InOutCodecsEndpoint[In, Out] =
//      InOutCodecsEndpoint(method, route, inCodec, outCodec)
//
//  case class InOutCodecEndpoint[In, Out](method: Method, route: String, outCodec: JsonCodec[Out])
//      extends IncompleteEndpoint[In, Out]:
//    def withInCodec(using inCodec: JsonCodec[In]): InOutCodecsEndpoint[In, Out] =
//      InOutCodecsEndpoint(method, route, inCodec, outCodec)
//
//  case class InOutCodecsEndpoint[In, Out](
//    method: Method,
//    route: String,
//    inCodec: JsonCodec[In],
//    outCodec: JsonCodec[Out],
//  ) extends Endpoint[In, Out]
//
//  case class EndpointWithResolver[-R, In, Out](endpoint: Endpoint[In, Out], resolver: In => ZIO[R, Throwable, Out])

  object example:
    val echo: AnyUnitEndpoint[(String, Int)] = Endpoint.get(Route.init / "echo" / StringParam / IntParam)

object v4 extends App:

  import Param.*

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

  sealed trait Route[-In]

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

  enum Param[A]:
    case IntParam extends Param[Int]
    case StringParam extends Param[String]

  //    override def toString: String = this match {
  //      case IntParam    => "<<Int>>"
  //      case StringParam => "<<String>>"
  //    }

  sealed trait ParamsEndpoint[PathParams, Ro <: Route[PathParams], In, Out]:
    val method: Method
    val route: Ro
    def resolveWith[R](f: ((PathParams, In)) => ZIO[R, Throwable, Out]) = ???

  sealed trait Endpoint[In, Out]:
    val method: Method
    val route: Route0

  sealed trait IncompleteEndpoint[In, Out]
  sealed trait IncompleteParamsEndpoint[R <: Route[_], In, Out]

  object Endpoint:
    def get(route: String)                                          = AnyUnitEndpoint(Method.GET, Route.init / route)
    def get(route: Route0)                                          = AnyUnitEndpoint(Method.GET, route)
    def get[A](route: Route[A]): ParamsAnyUnitEndpoint[A, Route[A]] = ParamsAnyUnitEndpoint(Method.GET, route)
  //    def post(route: String)      = AnyUnitEndpoint(Method.POST, route)
  //    def post[A](route: Route[A]) = WithPathParams[A](Method.POST, route)

  case class AnyUnitEndpoint(method: Method, route: Route0) extends Endpoint[Any, Unit]:
    def in[In]: InEndpoint[In] = InEndpoint[In](method, route)
  //    def out[Out]: OutEndpoint[PathParams, Out] = OutEndpoint[PathParams, Out](method, route)

  case class InEndpoint[In](method: Method, route: Route0) extends IncompleteEndpoint[In, Unit]:
    def withInCodec(using inCodec: JsonCodec[In]): InCodecEndpoint[In] =
      InCodecEndpoint(method, route, inCodec)

    case class InCodecEndpoint[In](method: Method, route: Route0, inCodec: JsonCodec[In]) extends Endpoint[In, Unit]
  //    def out[Out]: InCodecOutEndpoint[In, Out] = InCodecOutEndpoint[In, Out](method, route, inCodec)
  //

  case class ParamsAnyUnitEndpoint[P, R <: Route[P]](method: Method, route: R) extends ParamsEndpoint[P, R, Any, Unit]:
    def in[In]: ParamsInEndpoint[P, R, In] = ParamsInEndpoint[P, R, In](method, route)
  //    def out[Out] = InOutEndpoint[PathParams, In, Out](method, route)

  case class ParamsInEndpoint[P, R <: Route[P], In](method: Method, route: R)
      extends IncompleteParamsEndpoint[R, In, Unit]:
    def withInCodec(using inCodec: JsonCodec[In]): ParamsInCodecEndpoint[P, R, In] =
      ParamsInCodecEndpoint(method, route, inCodec)

  case class ParamsInCodecEndpoint[P, R <: Route[P], In](method: Method, route: R, inCodec: JsonCodec[In])
      extends ParamsEndpoint[P, R, In, Unit]

  //  case class OutEndpoint[PathParams, Out](method: Method, route: Route[PathParams])
  //      extends IncompleteEndpoint[PathPArams, Any, Out]:
  //    def withOutCodec(using outCodec: JsonCodec[Out]): OutCodecEndpoint[Out] =
  //      OutCodecEndpoint(method, route, outCodec)
  //    def in[In]: InOutEndpoint[In, Out] = InOutEndpoint[In, Out](method, route)
  //
  //  case class InOutEndpoint[In, Out](method: Method, route: String) extends IncompleteEndpoint[In, Out]:
  //    def withInCodec(using inCodec: JsonCodec[In]): InCodecOutEndpoint[In, Out] =
  //      InCodecOutEndpoint(method, route, inCodec)
  //    def withOutCodec(using outCodec: JsonCodec[Out]): InOutCodecEndpoint[In, Out] =
  //      InOutCodecEndpoint(method, route, outCodec)
  //
  //  case class OutCodecEndpoint[Out](method: Method, route: String, outCodec: JsonCodec[Out]) extends Endpoint[Any, Out]:
  //    def in[In]: InOutCodecEndpoint[In, Out] = InOutCodecEndpoint[In, Out](method, route, outCodec)
  //
  //  case class InCodecOutEndpoint[In, Out](method: Method, route: String, inCodec: JsonCodec[In])
  //      extends IncompleteEndpoint[In, Out]:
  //    def withOutCodec(using outCodec: JsonCodec[Out]): InOutCodecsEndpoint[In, Out] =
  //      InOutCodecsEndpoint(method, route, inCodec, outCodec)
  //
  //  case class InOutCodecEndpoint[In, Out](method: Method, route: String, outCodec: JsonCodec[Out])
  //      extends IncompleteEndpoint[In, Out]:
  //    def withInCodec(using inCodec: JsonCodec[In]): InOutCodecsEndpoint[In, Out] =
  //      InOutCodecsEndpoint(method, route, inCodec, outCodec)
  //
  //  case class InOutCodecsEndpoint[In, Out](
  //    method: Method,
  //    route: String,
  //    inCodec: JsonCodec[In],
  //    outCodec: JsonCodec[Out],
  //  ) extends Endpoint[In, Out]
  //
  //  case class EndpointWithResolver[-R, In, Out](endpoint: Endpoint[In, Out], resolver: In => ZIO[R, Throwable, Out])

  object example:
    val echo: ParamsAnyUnitEndpoint[(String, Int), Route[(String, Int)]] =
      Endpoint.get(Route.init / "echo" / StringParam / IntParam)
