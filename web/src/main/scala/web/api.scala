package web

import core.*
import endpoints.*
import zio.json.*

object api extends codecs:

  object catalog:
    val all      = Endpoint.get("catalog").out[List[Ident[Article]]]
    val find     = Endpoint.get(Route.init / "catalog" / StringParam).out[Ident[Article]]
    val add      = Endpoint.post("catalog").in[AddArticleForm].out[Ident[Article]]
    val remove   = Endpoint.delete(Route.init / "catalog" / StringParam).out[Ident[Article]]
    val setPrice = Endpoint.patch(Route.init / "catalog" / StringParam / "price" / StringParam).out[Ident[Article]]

  object orders:
    val all     = Endpoint.get("orders").out[List[Ident[Order]]]
    val find    = Endpoint.get(Route.init / "orders" / StringParam).out[Ident[Order]]
    val place   = Endpoint.post("orders").in[PlaceOrderForm].out[Ident[Order]]
    val pay     = Endpoint.patch(Route.init / "orders" / StringParam / "pay").out[Ident[Order]]
    val deliver = Endpoint.patch(Route.init / "orders" / StringParam / "deliver").out[Ident[Order]]
    val close   = Endpoint.patch(Route.init / "orders" / StringParam / "close").out[Ident[Order]]
    val cancel  = Endpoint.patch(Route.init / "orders" / StringParam / "cancel").out[Ident[Order]]

trait codecs:

  given JsonDecoder[NonEmptyString] = JsonDecoder.string.mapOrFail(NonEmptyString.apply(_).left.map(_.getMessage))
  given JsonEncoder[NonEmptyString] = JsonEncoder.string.contramap(_.self)
  given JsonDecoder[ARS.Price] =
    JsonDecoder.string.mapOrFail(s => ARS.fromString(s).toRight(s"Couldn't parse <<$s>> as an ARS Price"))
  given JsonEncoder[ARS.Price]    = JsonEncoder.string.contramap(_.toString)
  given JsonCodec[Article]        = DeriveJsonCodec.gen
  given JsonCodec[AddArticleForm] = DeriveJsonCodec.gen

  implicit def identCodec[A: JsonCodec]: JsonCodec[Ident[A]] = DeriveJsonCodec.gen
  implicit def nonEmptySetEncoder[A: JsonEncoder]: JsonEncoder[NonEmptySet[A]] =
    JsonEncoder.apply[Set[A]].contramap(_.self)
  implicit def nonEmptySetDecoder[A: JsonDecoder]: JsonDecoder[NonEmptySet[A]] =
    JsonDecoder.apply[Set[A]].mapOrFail(set => NonEmptySet(set.toSeq: _*).left.map(_.getMessage))

  given JsonEncoder[Nat]          = JsonEncoder.int.contramap(_.self)
  given JsonDecoder[Nat]          = JsonDecoder.int.mapOrFail(Nat(_).left.map(_.getMessage))
  given JsonCodec[Item]           = DeriveJsonCodec.gen
  given JsonCodec[Customer]       = DeriveJsonCodec.gen
  given JsonCodec[Status]         = DeriveJsonCodec.gen
  given JsonCodec[Order]          = DeriveJsonCodec.gen
  given JsonCodec[PlaceOrderForm] = DeriveJsonCodec.gen
