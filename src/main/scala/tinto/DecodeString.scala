package tinto

trait DecodeString[T]:
  def decode(input: String): Option[T]
