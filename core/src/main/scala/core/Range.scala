package core

sealed trait Range[T <: Ordered[T]]:
  def contains(element: T): Boolean
  def &&(that: Range[T]): Range[T]

object Range:

  def fromString[T <: Ordered[T]: DecodeString](input: String): Option[Range[T]] =
    import aconcagua.std.list.syntax.traverse
    input
      .strip()
      .split(',')
      .map(fromStringSingle)
      .toList
      .traverse
      .flatMap(_.reduceOption(_ && _))

  private def fromStringSingle[T <: Ordered[T]: DecodeString](input: String): Option[Range[T]] = input.strip() match {
    case OpeningRange(range) => Some(range)
    case ClosingRange(range) => Some(range)
    case _                   => None
  }

  private object OpeningRange:
    def unapply[T <: Ordered[T]: DecodeString](input: String): Option[Range[T]] =
      val (first, second) = input.splitAt(1)
      (first.strip(), second.strip()) match {
        case (_, "...")   => Some(All())
        case ("[", value) => summon[DecodeString[T]].decode(value).map(GreaterOrEqualsThan.apply)
        case _            => None
      }

  private object ClosingRange:
    def unapply[T <: Ordered[T]: DecodeString](input: String): Option[Range[T]] =
      val (first, second) = input.splitAt(input.length - 1)
      (first.strip(), second.strip()) match {
        case ("...", _)   => Some(All())
        case (value, ")") => summon[DecodeString[T]].decode(value).map(LessThan.apply)
        case _            => None
      }

case class InclusiveExclusive[T <: Ordered[T]](lowerInclusive: T, upperExclusive: T) extends Range[T]:
  override def contains(element: T): Boolean = lowerInclusive <= element && element < upperExclusive
  override def toString: String              = s"[$lowerInclusive, $upperExclusive)"
  override def &&(that: Range[T]): Range[T] = that match {
    case InclusiveExclusive(lowerInclusive, upperExclusive) =>
      InclusiveExclusive(max(this.lowerInclusive, lowerInclusive), min(this.upperExclusive, upperExclusive))
    case GreaterOrEqualsThan(lowerInclusive) =>
      InclusiveExclusive(max(this.lowerInclusive, lowerInclusive), this.upperExclusive)
    case LessThan(upperExclusive) =>
      InclusiveExclusive(lowerInclusive, min(this.upperExclusive, upperExclusive))
    case All() => this
  }

case class GreaterOrEqualsThan[T <: Ordered[T]](lowerInclusive: T) extends Range[T]:
  override def contains(element: T): Boolean = lowerInclusive <= element
  override def toString: String              = s"[$lowerInclusive, ...)"
  override def &&(that: Range[T]): Range[T] = that match {
    case InclusiveExclusive(lowerInclusive, upperExclusive) =>
      if this.lowerInclusive < lowerInclusive then that else InclusiveExclusive(this.lowerInclusive, upperExclusive)
    case GreaterOrEqualsThan(lowerInclusive) =>
      if this.lowerInclusive < lowerInclusive then that else this
    case LessThan(upperExclusive) =>
      InclusiveExclusive(lowerInclusive, upperExclusive)
    case All() =>
      this
  }

case class LessThan[T <: Ordered[T]](upperExclusive: T) extends Range[T]:
  override def contains(element: T): Boolean = element < upperExclusive
  override def toString: String              = s"(..., $upperExclusive)"
  override def &&(that: Range[T]): Range[T] = that match {
    case InclusiveExclusive(lowerInclusive, upperExclusive) =>
      if upperExclusive < this.upperExclusive then that else InclusiveExclusive(lowerInclusive, this.upperExclusive)
    case GreaterOrEqualsThan(lowerInclusive) =>
      InclusiveExclusive(lowerInclusive, upperExclusive)
    case LessThan(upperExclusive) =>
      LessThan(min(upperExclusive, this.upperExclusive))
    case All() =>
      this
  }

case class All[T <: Ordered[T]]() extends Range[T]:
  override def contains(element: T): Boolean = true
  override def toString: String              = s"(..., ...)"
  override def &&(that: Range[T]): Range[T]  = that

private def min[T <: Ordered[T]](a: T, b: T): T = if a < b then a else b
private def max[T <: Ordered[T]](a: T, b: T): T = if b < a then a else b
