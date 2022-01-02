package tinto

import zio.ZIO
import zio.stream.ZStream

import java.util.UUID

// TODO echo input or updated item
trait Database[T]:
  def insert(id: UUID, element: T): ZIO[Any, Error, Ident[T]]
  def update(id: UUID, f: T => T): ZIO[Any, Error, Ident[T]]
  def updateEither(id: UUID, f: T => Either[Error, T]): ZIO[Any, Error, Ident[T]]
  def delete(id: UUID): ZIO[Any, Error, Ident[T]]
  def find(id: UUID): ZIO[Any, Error, Ident[T]]
  def all: ZStream[Any, Error, Ident[T]]
  def filter(filter: Filter[T]): ZStream[Any, Error, Ident[T]]
