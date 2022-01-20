package frontend

import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L.*
import mira.*
import zio.console.putStrLn
import zio.{Runtime, ZEnv}
import client.scalajs.client.fetch
import web.v4.{IntParam, Route, StringParam}

object Main:

  private val nubeVar      = Var("nube var vacia")
  private val echoEndpoint = web.Endpoint.get(Route.init / "echo" / StringParam).out[String]

  val a      = web.Endpoint.get(Route.init / "users" / StringParam / "posts" / IntParam / StringParam)
  val fetchA = fetch(a)

  private val echo = fetch(echoEndpoint)

  implicit val runtime: Runtime[ZEnv] = Runtime.default

  private val fetchZIO =
    import zio.duration.*
    Shape
      .text(nubeVar.signal)
      .onClick(echo("hola nuvolina", ()).tap(putStrLn(_)).delay(1.second).map(nubeVar.set).ignore)
//      .onClick(fetchA(("hola", 13, "nuvolina"), ()).ignore)

//  private val fetchNoZIO =
//    import client.scalajs.syntax.fetch
//    Shape
//      .text(nubeVar.signal)
//      .onClick(runtime unsafeRunAsync_ echoEndpoint.fetch("hola nuvolina no zio").map(nubeVar.set))

  def main(args: Array[String]): Unit =
    renderOnDomContentLoaded(
      org.scalajs.dom.document.querySelector("#app"),
      div(
        fetchZIO.build,
//        fetchNoZIO.build,
        example.root.build,
      ),
    )
