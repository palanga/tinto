package frontend

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.{Var, documentEvents, render, unsafeWindowOwner, renderOnDomContentLoaded}
import org.scalajs.dom
import org.scalajs.dom.KeyCode
import org.scalajs.dom.window.alert

object Main:

  def main(args: Array[String]): Unit = renderOnDomContentLoaded(dom.document.querySelector("#app"), example.root.build)
