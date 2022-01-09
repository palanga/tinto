package frontend

import com.raquo.domtypes.generic.Modifier
import com.raquo.domtypes.generic.builders.HtmlTagBuilder
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.html
import org.scalajs.dom.window.alert
import zio.*

object Main:

  def main(args: Array[String]): Unit = {
    val _ = documentEvents.onDomContentLoaded.foreach { _ =>
      val appContainer = dom.document.querySelector("#app")
      appContainer.innerHTML = ""
      val _ = render(appContainer, view)
    }(unsafeWindowOwner)
  }

  def view =
    elements.Element
      .of(
        elements.Element.of("holanda").onClick(() => alert("interno")),
        elements.Element.of("como andaira"),
        elements.Element.of(clicks.signal),
        elements.Element.of("si me apretas aumenta el numero").onClick(clicks.update(_ + 1)),
        elements.Element.empty.onInput(text.set),
        elements.Element.of(text.signal),
      )
      .build

  val clicks: Var[Int]  = Var(0)
  val text: Var[String] = Var("escribí algo")

object elements:

  type LaminarElem = ReactiveHtmlElement[_ <: dom.html.Element]
  type LaminarMod  = Modifier[LaminarElem]

  sealed trait Element(modifiers: List[LaminarMod]):
    def onClick(f: => Any): Element = this.addModifier(L.onClick --> { _ => f })
    def build: LaminarElem
    private[elements] def addModifier(modifier: LaminarMod): Element

  object Element:
    val empty: Node                                  = Node("")
    def of(text: String): Node                       = Node(text)
    def of(signal: Signal[Any]): Node                = empty.bind(signal)
    def of(child: Element, children: Element*): Edge = Edge(child, children)

    case class Node(
      private val text: String,
      private val modifiers: List[LaminarMod] = Nil,
      private val builder: (String, List[LaminarMod]) => LaminarElem = (text, modifiers) => L.div(text, modifiers),
    ) extends Element(modifiers):

      def bind(signal: Signal[Any]): Node = this.addModifier(child.text <-- signal.map(_.toString))

      def onInput(f: String => Any): Node =
        val mod: LaminarMod = L.onInput.mapToValue --> { f(_) }
        this.copy(builder = (text, modifiers) => L.input(text, modifiers), modifiers = mod :: modifiers)

      override def build: LaminarElem = builder(text, modifiers)

      override def addModifier(modifier: LaminarMod): Node = this.copy(modifiers = modifier :: modifiers)

    case class Edge(
      private val child: Element,
      private val children: Seq[Element] = Nil,
      private val modifiers: List[LaminarMod] = Nil,
    ) extends Element(modifiers):
      override def build: LaminarElem                         = L.div(child.build, children.map(_.build), modifiers)
      override def addModifier(modifier: LaminarMod): Element = this.copy(modifiers = modifier :: modifiers)
