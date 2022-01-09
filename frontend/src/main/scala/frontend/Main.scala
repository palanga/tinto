package frontend

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.{Var, documentEvents, render, unsafeWindowOwner}
import org.scalajs.dom
import org.scalajs.dom.window.alert

object Main:

  def main(args: Array[String]): Unit = {
    val _ = documentEvents.onDomContentLoaded.foreach { _ =>
      val appContainer = dom.document.querySelector("#app")
      appContainer.innerHTML = ""
      val _ = render(appContainer, view.build)
    }(unsafeWindowOwner)
  }

  def view =
    Element
      .of(
        Element.of("holanda").onClick(() => alert("co tas")),
        Element.of("+").onClick(clicks.update(_ + 1)),
        Element.of(clicks.signal),
        Element.of("-").onClick(clicks.update(_ - 1)),
        Element.of(text.signal).onInput(text.set),
        Element.of("agregar").onClick { names.update(text.now() :: _); text.set("") },
        Element.ofMany(names.signal.map(_.map(perri))),
      )

  def updateAndErase = {
    names.update(text.now() :: _); text.set("")
  }

  def perri(name: String) =
    Element.of(
      Element.of("-").onClick(names.update(_.filterNot(_ == name))),
      Element.of(s"el perri se llama $name"),
    )

  val clicks: Var[Int]         = Var(0)
  val text: Var[String]        = Var("escribí algo")
  val names: Var[List[String]] = Var("Nube" :: "Canela" :: "Rocco" :: Nil)
