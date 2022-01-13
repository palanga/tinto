package frontend

import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import org.scalajs.dom
import org.scalajs.dom.window.alert
import org.scalajs.dom.{KeyCode, console, fetch}

object Main:

  val eventsVar = Var(List.empty[String])

  def main(args: Array[String]): Unit =
    renderOnDomContentLoaded(
      dom.document.querySelector("#app"),
//      example.root.build
      div(
        b,
        div(children <-- eventsVar.signal.map(_.map(div(_)))),
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
