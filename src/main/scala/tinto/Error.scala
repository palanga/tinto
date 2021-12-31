package tinto

import java.util.UUID

enum Error extends Throwable:
  case AlreadyPaid, AlreadyDelivered, OrderClosed, ContactInfoIsBlank, InvalidAddress, EmptyItemList, EmptyTitle,
  LessOrEqualToZero
  case NotFound(id: UUID)

  override def getMessage: String = this match {
    case AlreadyPaid        => "AlreadyPaid"
    case AlreadyDelivered   => "AlreadyDelivered"
    case OrderClosed        => "OrderClosed"
    case ContactInfoIsBlank => "ContactInfoIsBlank"
    case InvalidAddress     => "InvalidAddress"
    case EmptyItemList      => "EmptyItemList"
    case EmptyTitle         => "EmptyTitle"
    case LessOrEqualToZero  => "LessOrEqualToZero"
    case NotFound(id)       => s"Element with id <<$id>> not found"
  }
