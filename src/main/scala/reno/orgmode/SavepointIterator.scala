package reno.orgmode

import cats.implicits._

import scala.collection.IterableOnce

case class SavepointIterator[A] private (var repr: Iterator[A]) extends IterableOnce[A] {
  private var history = none[List[A]]

  override def iterator: Iterator[A] = repr.iterator

  def nextOption(): Option[A] =
    repr.nextOption().map { next =>
      history = history.map(next +: _)
      next
    }

  def savepoint(): Unit =
    history = List.empty[A].some

  def rollback(): Either[String, Unit] =
    history
      .map { history =>
        val orig = repr
        this.repr = history.reverseIterator ++ orig
      }
      .toRight("`rollback()` failed - `savepoint()` missing")

}

object SavepointIterator {
  def fromIterableOnce[A](iterableOnce: IterableOnce[A]): SavepointIterator[A] =
    SavepointIterator(iterableOnce.iterator)
}
