package core

import java.util.UUID

case class Ident[T](id: UUID, self: T)
object Ident:
  def fromPair[T](tuple: (UUID, T)): Ident[T]           = Ident(tuple._1, tuple._2)
  extension [T](idElem: Ident[T]) def toPair: (UUID, T) = idElem.id -> idElem.self
  extension [T](pair: (UUID, T)) def asIdElem: Ident[T] = fromPair(pair)
  def unapply[T](idElem: Ident[T]): (UUID, T)           = idElem.toPair
