package frontend

import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
//import org.scalajs.dom
import org.scalajs.dom.window.alert
import org.scalajs.dom.{KeyCode, console, fetch}
import sttp.capabilities
import sttp.client3.{FetchBackend, SttpBackend}
import zio.{Has, ZIO, ZLayer}

import scala.concurrent.ExecutionContext

object Main:

  import zio.duration.*

  val eventsVar = Var(List.empty[String])

  val nubeVar = Var("nube var vacia")

  val echoEndpoint = web.Endpoint.post("echo").resolveWith((in: String) => ZIO.succeed(in))

  import client.syntax.fetch

  val runtime = zio.Runtime.default

  val backendLayer: ZLayer[Any, Nothing, Has[SttpBackend[scala.concurrent.Future, Any]]] =
    ZIO.succeed(FetchBackend()).toLayer

//TODO: probar con pasarle el ec cuando creamos el fetch backend
//  implicit val ec: ExecutionContext = runtime.platform.executor.asEC

  def main(args: Array[String]): Unit =
    import zio.duration.*
    runtime.unsafeRunAsync_(
      zio.console.putStrLn("fecheando")
        *>
          echoEndpoint
            .fetch("nuvolina")
            .provideCustomLayer(backendLayer)
            .tap(zio.console.putStrLn(_))
            .map(nubeVar.set)
            .timeout(2.seconds)
          *> zio.console.putStrLn("listo")
    )
    renderOnDomContentLoaded(
      org.scalajs.dom.document.querySelector("#app"),
//      example.root.build
      div(
        b,
        div(children <-- eventsVar.signal.map(_.map(div(_)))),
        div(
          child.text <-- nubeVar
        ),
      ),
    )

  val b = button(
    "Send",
    inContext { thisNode =>
      val $click = thisNode.events(onClick) /*.sample(selectedOptionVar.signal)*/
      val $response = $click.flatMap { opt =>
        articles_api.all
//          .recover { case err: AjaxStreamError => Some(err.getMessage) }
      }

      List(
//        $click.map(opt => List(s"Starting: GET ${opt.url}")) --> eventsVar,
        $response.map(_.toString) --> eventsVar.updater[String](_ :+ _)
      )
    },
  )
