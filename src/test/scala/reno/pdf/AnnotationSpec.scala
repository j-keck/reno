package reno.pdf

import cats.data.NonEmptyList
import org.scalacheck.Gen.posNum
import org.scalacheck.Prop.forAll
import org.scalacheck.Properties
import reno.RenoScalaCheckInstances._
import org.scalactic.TripleEquals._

object AnnotationSpec extends Properties("Annotation") {

  property("Ordering") = forAll(genAnnotation, posNum[Int], posNum[Int], posNum[Int]) {
    (a1, pageOffset, colOffset, rowOffset) =>
      def shiftPos(a: TextMarkupAnnotation, shift: Pos => Pos): TextMarkupAnnotation = {
        val rects = {
          val rect = a.mark.rects.head
          NonEmptyList.one(Rect2D(shift(rect.leftBottom), shift(rect.rightTop)))
        }
        a.copy(mark = a.mark.copy(rects = rects))
      }

      // shift column
      val a2 = shiftPos(a1, pos => pos.copy(col = pos.col + colOffset))

      // shift row
      val a3 = shiftPos(a1, pos => pos.copy(row = pos.row + rowOffset))

      // shift page
      val a4 = a1.copy(pageNumber = a1.pageNumber + pageOffset)

      List(a4, a3, a1, a2).sorted === List(a1, a2, a3, a4)
  }
}
