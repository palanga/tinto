package frontend

import com.raquo.domtypes.generic.Modifier
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.html

sealed trait Element(modifiers: List[LaminarMod]):
  def onClick(f: => Any): Element = this.addModifier(L.onClick --> { _ => f })
  def build: LaminarElem
  protected def addModifier(modifier: LaminarMod): Element

object Element:
  val empty: Node                                  = Node("")
  val input: Node                                  = Node("")
  def of(text: String): Node                       = Node(text)
  def ofMany(signal: Signal[List[Element]]): Node  = empty.bindAll(signal)
  def of(signal: Signal[Any]): Node                = empty.bind(signal)
  def of(child: Element, children: Element*): Edge = Edge(child, children)

  case class Node(
    private val text: String,
    private val modifiers: List[LaminarMod] = Nil,
    private val builder: (String, List[LaminarMod]) => LaminarElem = (text, modifiers) => L.div(text, modifiers),
  ) extends Element(modifiers):

    def bindAll(signal: Signal[Seq[Element]]): Node =
      this.addModifier(children <-- signal.map(_.map(_.build)))

    def bind(signal: Signal[Any]): Node =
      this.addModifier(L.value <-- signal.map(_.toString)) // TODO esto no funciona para divs

    def onInput(f: String => Any): Node =
      val mod: LaminarMod = L.onInput.mapToValue --> { f(_) }
      this.copy(
        builder = (text, modifiers) => L.input(text, modifiers),
        modifiers = mod :: modifiers,
      )

    override def build: LaminarElem = builder(text, modifiers)

    override def addModifier(modifier: LaminarMod): Node = this.copy(modifiers = modifier :: modifiers)

  case class Edge(
    private val child: Element,
    private val children: Seq[Element] = Nil,
    private val modifiers: List[LaminarMod] = Nil,
  ) extends Element(modifiers):
    override def build: LaminarElem                         = L.div(child.build, children.map(_.build), modifiers)
    override def addModifier(modifier: LaminarMod): Element = this.copy(modifiers = modifier :: modifiers)

private type LaminarElem = ReactiveHtmlElement[_ <: dom.html.Element]
private type LaminarMod  = Modifier[LaminarElem]
