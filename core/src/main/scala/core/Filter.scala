package core

trait Filter[T]:
  def contains(element: T): Boolean
