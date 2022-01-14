package core

object chaining:
  extension [A](self: A) def |>[B](f: A => B): B = f(self)

opaque type NonEmptyString = String
object NonEmptyString:
  def apply(s: String): Either[Error, NonEmptyString] = if s.isBlank then Left(Error.EmptyTitle) else Right(s)
  extension (t: NonEmptyString) def self: String      = t

opaque type NonEmptyList[T] = List[T]
object NonEmptyList:
  def apply[T](head: T, tail: T*): NonEmptyList[T] = head :: List(tail*)
  extension [A](list: NonEmptyList[A])
    def self: List[A]                                        = list
    def map[B](f: A => B): NonEmptyList[B]                   = list.map(f)
    def flatMap[B](f: A => NonEmptyList[B]): NonEmptyList[B] = list.flatMap(f)

opaque type NonEmptySet[T] = Set[T]
object NonEmptySet:
  def apply[T](head: T, tail: T*): NonEmptySet[T] = Set(tail*) + head
  def apply[T](elems: T*): Either[Error, NonEmptySet[T]] =
    if elems.isEmpty then Left(Error.EmptyItemList) else Right(NonEmptySet(elems.head, elems.tail*))
  extension [A](set: NonEmptySet[A])
    def self: Set[A]                                       = set
    def map[B](f: A => B): NonEmptySet[B]                  = set.map(f)
    def flatMap[B](f: A => NonEmptySet[B]): NonEmptySet[B] = set.flatMap(f)
    def reduce(f: (A, A) => A): A                          = set.reduce(f)

opaque type Nat = Int
object Nat:
  val ONE: Nat                          = 1
  def apply(n: Int): Either[Error, Nat] = if n <= 0 then Left(Error.LessOrEqualToZero) else Right(n)
  extension (a: Nat)
    def self: Int                   = a
    def +(b: Nat | Nat0): Nat       = a + b
    def -(b: Int | Nat | Nat0): Int = a - b

opaque type Nat0 = Int
object Nat0:
  val ZERO: Nat0                         = 0
  val ONE: Nat0                          = 1
  def apply(n: Int): Either[Error, Nat0] = if n <= 0 then Left(Error.LessOrEqualToZero) else Right(n)
  extension (a: Nat0)
    def self: Int                   = a
    def +(b: Nat | Nat0): Nat0      = a + b
    def -(b: Int | Nat | Nat0): Int = a - b
