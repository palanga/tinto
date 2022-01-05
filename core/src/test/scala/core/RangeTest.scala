package core

import zio.test.*
import zio.test.Assertion.*

object RangeTest extends DefaultRunnableSpec:

  given DecodeString[ARS.Price] with
    override def decode(input: String): Option[ARS.Price] = ARS.fromString(input)

  private def idOption(range: Range[ARS.Price]) = Range.fromString(range.toString)
  private val MIN                               = -10000.0
  private val MAX                               = 10000.0

  override def spec = fullSpec

  private val fullSpec = suite("range")(
    suite("from string")(
      testM("[a, b)") {
        checkAll(Gen.bigDecimal(MIN, MAX), Gen.bigDecimal(MIN, MAX)) { (lower, upper) =>
          val range = InclusiveExclusive(ARS * lower, ARS * upper)
          assert(idOption(range))(isSome(equalTo(range)))
        }
      },
      testM("[a, ...)") {
        checkAll(Gen.bigDecimal(MIN, MAX)) { lower =>
          val range = GreaterOrEqualsThan(ARS * lower)
          assert(idOption(range))(isSome(equalTo(range)))
        }
      },
      testM("(..., a]") {
        checkAll(Gen.bigDecimal(MIN, MAX)) { upper =>
          val range = LessThan(ARS * upper)
          assert(idOption(range))(isSome(equalTo(range)))
        }
      },
      test("(..., ...)") {
        val range = Range.fromString("(..., ...)")
        assert(range)(isSome(equalTo(All())))
      },
      testM("extra spaces") {
        checkAll(Gen.bigDecimal(MIN, MAX), Gen.bigDecimal(MIN, MAX)) { (lower, upper) =>
          val range = Range.fromString(s"    [   ARS     $lower    ,    ARS     $upper    )    ")
          val all   = Range.fromString("    (    ...      ,     ...      )     ")
          assert(range)(isSome(equalTo(InclusiveExclusive(ARS * lower, ARS * upper))))
          && assert(all)(isSome(equalTo(All())))
        }
      },
    )
  )
