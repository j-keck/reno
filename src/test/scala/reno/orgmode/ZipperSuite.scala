package reno.orgmode

import org.scalatest.funsuite.AnyFunSuite
import scala.collection.LinearSeq

class ZipperSuite extends AnyFunSuite {

  test("fromLinearSeq") {
    assert(Zipper.fromLinearSeq(LinearSeq.empty).isEmpty)
    assert(Zipper.fromLinearSeq(LinearSeq(1)).isDefined)
    assert(Zipper.fromLinearSeq(LinearSeq(1)).get.focus === 1)
  }

  test("next") {
    val z1 = Zipper(1, 2)
    assert(z1.focus === 1)

    val z2 = z1.next
    assert(z2.isDefined)
    assert(z2.get.focus === 2)

    val zn = z2.get.next
    assert(zn.isEmpty)
  }

  test("previous") {
    val z2 = Zipper(1, 2).next.get
    assert(z2.focus === 2)

    val z1 = z2.previous
    assert(z1.isDefined)
    assert(z1.get.focus === 1)

    val zn = z1.get.previous
    assert(zn.isEmpty)
  }

  test("peekNext") {
    val zipper = Zipper(1, 2)

    assert(zipper.focus === 1)
    assert(zipper.peekNext == Some(2))
    assert(zipper.next.get.peekNext == None)
  }

  test("nextWhile") {
    val Some(zipper) = Zipper(1, 2, 3, 4, 5).nextWhile(_ <= 3)
    assert(zipper.focus === 4)

    val Some(zipper0) = zipper.nextWhile(_ > 1000)
    assert(zipper0.focus === 4)

    assert(zipper0.nextWhile(_ => true).isEmpty)
  }

  test("takeWhile") {
    val zipper = Zipper(1, 2, 3, 4, 5)

    val (xs0, zipper0) = zipper.takeWhile(_ <= 3)
    assert(zipper0.focus === 4)
    assert(xs0 === List(1, 2, 3))

    val (xs1, zipper1) = zipper.takeWhile(_ => false)
    assert(zipper1.focus === 1)
    assert(xs1 === List.empty)
  }
}
