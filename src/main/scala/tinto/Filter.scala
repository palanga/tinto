package tinto

trait Filter[T]:
  def contains(element: T): Boolean
