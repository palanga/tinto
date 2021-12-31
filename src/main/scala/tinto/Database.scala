package tinto

import zio.ZIO
import zio.stream.ZStream

import java.util.UUID

trait Database[T]:
  def create(id: UUID, element: T): ZIO[Any, Error, UUID]
  def update(id: UUID, f: T => T): ZIO[Any, Error, UUID]
  def updateEither(id: UUID, f: T => Either[Error, T]): ZIO[Any, Error, UUID]
  def delete(id: UUID): ZIO[Any, Error, Unit]
  def find(id: UUID): ZIO[Any, Error, T]
  def all: ZStream[Any, Error, (UUID, T)]
  def filter(filter: Filter[T]): ZStream[Any, Error, (UUID, T)]
