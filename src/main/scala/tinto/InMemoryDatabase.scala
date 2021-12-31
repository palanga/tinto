package tinto

import zio.ZIO
import zio.stm.{STM, TMap}
import zio.stream.ZStream

import java.util.UUID

object InMemoryDatabase:
  def init[T]: ZIO[Any, Nothing, InMemoryDatabase[T]] = TMap.empty[UUID, T].commit.map(InMemoryDatabase(_))

class InMemoryDatabase[T](val elements: TMap[UUID, T]) extends Database[T]:

  override def create(id: UUID, element: T): ZIO[Any, Error, UUID] =
    elements.put(id, element).commit.as(id)

  override def update(id: UUID, f: T => T): ZIO[Any, Error, UUID] =
    elements.get(id).someOrFail(Error.NotFound(id)).flatMap(elements.merge(id, _)((a, _) => f(a))).commit.as(id)

  override def updateEither(id: UUID, f: T => Either[Error, T]): ZIO[Any, Error, UUID] =
    (for {
      previous: T <- elements.get(id).someOrFail(Error.NotFound(id))
      current: T  <- STM.fromEither(f(previous))
      _           <- elements.merge(id, current)((_, _) => current)
    } yield id).commit

  override def delete(id: UUID): ZIO[Any, Error, Unit] =
    elements.delete(id).commit.unit

  override def find(id: UUID): ZIO[Any, Error, T] =
    elements.get(id).commit.someOrFail(Error.NotFound(id))

  override def all: ZStream[Any, Error, (UUID, T)] =
    ZStream.fromIterableM(elements.toChunk.commit)

  override def filter(filter: Filter[T]): ZStream[Any, Error, (UUID, T)] =
    all.filter((_, element) => filter.contains(element))
