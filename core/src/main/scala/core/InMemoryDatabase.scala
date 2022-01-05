package core

import zio.ZIO
import zio.stm.{STM, TMap}
import zio.stream.ZStream

import java.util.UUID

object InMemoryDatabase:
  def init[T]: ZIO[Any, Nothing, InMemoryDatabase[T]] = TMap.empty[UUID, T].commit.map(InMemoryDatabase(_))

class InMemoryDatabase[T](val elements: TMap[UUID, T]) extends Database[T]:

  override def insert(id: UUID, element: T): ZIO[Any, Error, Ident[T]] =
    elements.put(id, element).commit.as(Ident(id, element))

  override def update(id: UUID, f: T => T): ZIO[Any, Error, Ident[T]] =
    elements
      .get(id)
      .someOrFail(Error.NotFound(id))
      .flatMap(elements.merge(id, _)((a, _) => f(a)))
      .commit
      .map(Ident(id, _))

  override def updateEither(id: UUID, f: T => Either[Error, T]): ZIO[Any, Error, Ident[T]] =
    (for {
      previous: T <- elements.get(id).someOrFail(Error.NotFound(id))
      current: T  <- STM.fromEither(f(previous))
      _           <- elements.merge(id, current)((_, _) => current)
    } yield Ident(id, current)).commit

  override def delete(id: UUID): ZIO[Any, Error, Ident[T]] =
    elements
      .get(id)
      .someOrFail(Error.NotFound(id))
      .flatMap(element => elements.delete(id).as(Ident(id, element)))
      .commit

  override def find(id: UUID): ZIO[Any, Error, Ident[T]] =
    elements.get(id).commit.someOrFail(Error.NotFound(id)).map(Ident(id, _))

  override def all: ZStream[Any, Error, Ident[T]] =
    ZStream.fromIterableM(elements.toChunk.commit).map(Ident.fromPair)

  override def filter(filter: Filter[T]): ZStream[Any, Error, Ident[T]] =
    all.filter { case Ident(_, element) => filter.contains(element) }
