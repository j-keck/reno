package reno

import java.nio.file.Paths

import cats.data.NonEmptyList
import cats.implicits._
import org.scalacheck.{Arbitrary, Gen}
import org.scalactic.Equality
import org.scalactic.Tolerance._
import org.scalactic.TripleEquals._
import reno.ScalaCheckCatsInstances._
import reno.pdf.{Mark, PdfInfo, Pos, Rect2D, TextMarkupAnnotation}

object RenoScalaCheckInstances {

  lazy val genPdfInfo: Gen[PdfInfo] = for {
    title    <- Gen.alphaStr
    author   <- Gen.alphaStr
    subject  <- Gen.alphaStr
    created  <- Gen.calendar
    keywords <- Gen.listOf(Gen.alphaStr).map(_.mkString(", "))
    path     <- Gen.alphaStr.map(Paths.get(_))
  } yield PdfInfo(title, author, subject, created, keywords, path)
  implicit lazy val arbPdfInfo: Arbitrary[PdfInfo] = Arbitrary(genPdfInfo)

  lazy val genPos: Gen[Pos]                = (Gen.posNum[Float], Gen.posNum[Float]).mapN { case (row, col) => Pos(row, col) }
  implicit lazy val arbPos: Arbitrary[Pos] = Arbitrary(genPos)

  lazy val genRect2D: Gen[Rect2D]                = (genPos, genPos).mapN { case (lb, rt) => Rect2D(lb, rt) }
  implicit lazy val arbRect2D: Arbitrary[Rect2D] = Arbitrary(genRect2D)

  lazy val genMark: Gen[Mark] = (Gen.alphaStr, genRect2D).mapN {
    case (id, rect) =>
      Mark(id, NonEmptyList.one(rect))
  }
  implicit lazy val arbMark: Arbitrary[Mark] = Arbitrary(genMark)

  lazy val genAnnotation: Gen[TextMarkupAnnotation] = (Gen.posNum[Int], genMark, Gen.alphaStr).mapN {
    case (pageNumber, mark, text) =>
      TextMarkupAnnotation(pageNumber, mark, text)
  }
  implicit lazy val arbAnnotation: Arbitrary[TextMarkupAnnotation] = Arbitrary(genAnnotation)

  implicit lazy val equalityPos: Equality[Pos] = (a: Pos, b: Any) =>
    b match {
      case b: Pos =>
        val tolerance = 0.00001f
        a.y === b.y +- tolerance && a.x === b.x +- tolerance
      case _ => false
    }
}
