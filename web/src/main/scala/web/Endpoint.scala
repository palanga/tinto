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

  private val noop: Any => ZIO[Any, Throwable, Unit] = _ => ZIO.unit

enum Method:
  case GET, POST
