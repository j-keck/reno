package reno.orgmode

import cats.implicits._

import scala.collection.LinearSeq

final case class Zipper[A] private (
    left: LinearSeq[A],
    focus: A,
    right: LinearSeq[A]
) {

  def toLinearSeq: LinearSeq[A] = left.reverse ++ (focus +: right)

  def toList: List[A] = toLinearSeq.toList

  def next: Option[Zipper[A]] =
    right match {
      case LinearSeq() => none
      case r :: rs     => Zipper(left :+ focus, r, rs).some
    }

  def previous: Option[Zipper[A]] =
    left match {
      case LinearSeq() => none
      case l :: ls     => Zipper(ls, l, focus +: right).some
    }

  def focusNext: Option[(A, Zipper[A])] =
    next.map(zipper => (zipper.focus, zipper))

  def focusPrevious: Option[(A, Zipper[A])] =
    previous.map(zipper => (zipper.focus, zipper))

  def peekNext: Option[A] =
    right match {
      case LinearSeq() => none
      case r :: _      => r.some
    }

  def peekPrevious: Option[A] =
    left match {
      case LinearSeq() => none
      case l :: _      => l.some
    }

  def nextWhile(p: A => Boolean): Option[Zipper[A]] =
    if (p(focus)) next.flatMap(_.nextWhile(p)) else this.some

  def takeWhile(p: A => Boolean): (List[A], Zipper[A]) =
    if (p(focus)) {
      val (as, zipper0) = next.fold((List.empty[A], this))(_.takeWhile(p))
      (focus :: as, zipper0)
    } else (Nil, this)

  def exists(p: A => Boolean): Boolean = toList.exists(p)

  def idx: Int    = left.length
  def length: Int = left.length + 1 + right.length
}
object Zipper {

  def apply[A](focus: A, rest: A*): Zipper[A] = Zipper(LinearSeq.empty, focus, rest.to(LinearSeq))

  def fromLinearSeq[A](seq: LinearSeq[A]): Option[Zipper[A]] =
    seq match {
      case f :: right => Zipper(LinearSeq.empty, f, right).some
      case _          => none
    }

  def fromIterableOnce[A](iter: IterableOnce[A]): Option[Zipper[A]] =
    fromLinearSeq(iter.iterator.to(LinearSeq))
}
