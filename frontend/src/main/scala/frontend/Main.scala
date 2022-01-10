package frontend

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.{Var, documentEvents, render, unsafeWindowOwner}
import org.scalajs.dom
import org.scalajs.dom.KeyCode
import org.scalajs.dom.window.alert

object Main:

  def main(args: Array[String]): Unit = {
    val _ = documentEvents.onDomContentLoaded.foreach { _ =>
      val appContainer = dom.document.querySelector("#app")
      appContainer.innerHTML = ""
      val _ = render(appContainer, example.view.build)
    }(unsafeWindowOwner)
  }
