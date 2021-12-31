package tinto

import zio.test.*
import zio.test.Assertion.*

object RangeTest extends DefaultMutableRunnableSpec:

  given DecodeString[ARS.Price] with
    override def decode(input: String): Option[ARS.Price] = ARS.fromString(input)

  private def idOption(range: tinto.Range[ARS.Price]) = tinto.Range.fromString(range.toString)
  private val MIN                                     = -10000.0
  private val MAX                                     = 10000.0

  suite("range") {

    suite("from string") {

      testM("[a, b)") {
        checkAll(Gen.bigDecimal(MIN, MAX), Gen.bigDecimal(MIN, MAX)) { (lower, upper) =>
          val range = InclusiveExclusive(ARS * lower, ARS * upper)
          assert(idOption(range))(isSome(equalTo(range)))
        }
      }

      testM("[a, ...)") {
        checkAll(Gen.bigDecimal(MIN, MAX)) { lower =>
          val range = GreaterOrEqualsThan(ARS * lower)
          assert(idOption(range))(isSome(equalTo(range)))
        }
      }

      testM("(..., a]") {
        checkAll(Gen.bigDecimal(MIN, MAX)) { upper =>
          val range = LessThan(ARS * upper)
          assert(idOption(range))(isSome(equalTo(range)))
        }
      }

      test("(..., ...)") {
        val range = tinto.Range.fromString("(..., ...)")
        assert(range)(isSome(equalTo(All())))
      }

      testM("extra spaces") {
        checkAll(Gen.bigDecimal(MIN, MAX), Gen.bigDecimal(MIN, MAX)) { (lower, upper) =>
          val range = tinto.Range.fromString(s"    [   ARS     $lower    ,    ARS     $upper    )    ")
          val all   = tinto.Range.fromString("    (    ...      ,     ...      )     ")
          assert(range)(isSome(equalTo(InclusiveExclusive(ARS * lower, ARS * upper))))
          && assert(all)(isSome(equalTo(All())))
        }
      }

    }

  }
