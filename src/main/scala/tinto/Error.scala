package tinto

import java.util.UUID

enum Error extends Throwable:
  case ContactInfoIsBlank, InvalidAddress, EmptyItemList, EmptyTitle, LessOrEqualToZero
  case IllegalTransition(current: Status, next: Status)
  case NotFound(id: UUID)

  override def getMessage: String = this match {
    case IllegalTransition(current, next) => s"Can't transition from $current to $next"
    case ContactInfoIsBlank               => "ContactInfoIsBlank"
    case InvalidAddress                   => "InvalidAddress"
    case EmptyItemList                    => "EmptyItemList"
    case EmptyTitle                       => "EmptyTitle"
    case LessOrEqualToZero                => "LessOrEqualToZero"
    case NotFound(id)                     => s"Element with id <<$id>> not found"
  }
