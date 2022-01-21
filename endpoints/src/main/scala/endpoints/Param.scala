package endpoints

sealed trait Param[A]:
  def fromStringUnsafe(input: String): A = this match {
    case IntParam    => decode(_.toInt)(input)
    case StringParam => decode(identity)(input)
  }

  override def toString: String = this match {
    case IntParam    => "Int"
    case StringParam => "String"
  }

  private def decode[A](transform: String => A)(input: String) = transform(java.net.URLDecoder.decode(input, "UTF-8"))

case object IntParam    extends Param[Int]
case object StringParam extends Param[String]
