package reno.orgmode

import cats.syntax.either._
import org.scalatest.funsuite.AnyFunSuite

class SavepointIteratorSuite extends AnyFunSuite {

  test("`savepoint()` / `rollback()`") {

    val iter = SavepointIterator.fromIterableOnce(1 to 50)

    // consume the first 10 elements
    consumeAndCheck(1 to 10, iter)

    // take savepoint at element 10 - so the next would be 11
    iter.savepoint()

    // consume the next elements
    consumeAndCheck(11 to 30, iter)

    // rollback
    iter.rollback().leftMap(fail(_))

    // verify that the we start at 11 again
    consumeAndCheck(11 to 50, iter)

    // iterator should be now empty
    assert(iter.nextOption() === None)
  }

  test("`rollback()` without `savepoint()` should fail") {
    val iter = SavepointIterator.fromIterableOnce(1 to 10)
    assert(iter.rollback().isLeft)
  }

  // consumes the given iterator and validates the emitted values which the given values
  private def consumeAndCheck[A](range: Range, iter: SavepointIterator[A]) =
    assert(range === range.map(_ => iter.nextOption().getOrElse(fail("iterator was empty"))))
}
