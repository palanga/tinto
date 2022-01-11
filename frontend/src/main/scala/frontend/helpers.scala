package frontend

import org.scalajs.dom.console

import scala.scalajs.js.timers.setTimeout

def asynchronously(f: => Any): Unit = setTimeout(0)(f)

def debug(state: Any)(info: String)(f: => Any): Unit =
  console.group(info)
  console.debug("prev " + state.toString)
  f
  console.debug("next " + state.toString)
  console.groupEnd()
