package frontend

import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L.*
import mira.*
import zio.{Runtime, ZEnv}

object Main:

  private val nubeVar      = Var("nube var vacia")
  private val echoEndpoint = web.Endpoint.post("echo").resolveWith((in: String) => zio.ZIO.succeed(in))

  implicit val runtime: Runtime[ZEnv] = Runtime.default

  private val fetchZIO =
    import client.scalajs.syntax.fetch
    import zio.duration.*
    Shape
      .text(nubeVar.signal)
      .onClick(echoEndpoint.fetch("hola nuvolina").tap(zio.console.putStrLn(_)).delay(1.second).map(nubeVar.set).ignore)

  private val fetchNoZIO =
    import client.scalajs.syntax.fetch
    Shape
      .text(nubeVar.signal)
      .onClick(runtime unsafeRunAsync_ echoEndpoint.fetch("hola nuvolina no zio").map(nubeVar.set))

  def main(args: Array[String]): Unit =
    renderOnDomContentLoaded(
      org.scalajs.dom.document.querySelector("#app"),
      div(
        fetchZIO.build,
        fetchNoZIO.build,
        example.root.build,
      ),
    )
