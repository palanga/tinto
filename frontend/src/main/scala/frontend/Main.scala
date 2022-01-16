package frontend

import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L.*

object Main:

  private val nubeVar      = Var("nube var vacia")
  private val echoEndpoint = web.Endpoint.post("echo").resolveWith((in: String) => zio.ZIO.succeed(in))
  private val runtime      = zio.Runtime.default

  def main(args: Array[String]): Unit =
    renderOnDomContentLoaded(
      org.scalajs.dom.document.querySelector("#app"),
      root.build,
//      example.root.build
    )

  private val root =
    import client.scalajs.syntax.fetch
    Element
      .of(nubeVar.signal)
      .onClick(runtime unsafeRunAsync_ echoEndpoint.fetch("hola nuvolina").map(nubeVar.set))
