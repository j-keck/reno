package reno

import cats._
import org.scalacheck.Gen

object ScalaCheckCatsInstances {
  implicit def genFunctor: Functor[Gen] =
    new Functor[Gen] {
      override def map[A, B](fa: Gen[A])(f: A => B): Gen[B] = fa.map(f)
    }

  implicit def genSemigroupal: Semigroupal[Gen] =
    new Semigroupal[Gen] {
      override def product[A, B](fa: Gen[A], fb: Gen[B]): Gen[(A, B)] =
        for {
          a <- fa
          b <- fb
        } yield (a, b)
    }
}
