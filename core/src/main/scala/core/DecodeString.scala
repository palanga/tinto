package core

trait DecodeString[T]:
  def decode(input: String): Option[T]
