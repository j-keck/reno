package reno.pdf

import cats.data.NonEmptyList
import cats.syntax.list._
import org.apache.pdfbox.pdmodel._
import org.apache.pdfbox.pdmodel.interactive.annotation._

/**
  * Marked area in the pdf
  */
case class Mark private[reno] (id: String, rects: NonEmptyList[Rect2D]) {

  def onSameLines(other: Mark): Boolean = {
    val rows = this.rects.flatMap(r => NonEmptyList.of(r.leftBottom.row, r.rightTop.row))
    other.rects.exists(rect => rows.exists(row => row == rect.leftBottom.row || row == rect.rightTop.row))
  }

}

object Mark {

  def from(
      page: PDPage,
      annotation: PDAnnotationTextMarkup,
      from: Annotations.From
  ): Either[ProcessingPdfError, Mark] = {
    val id = annotation.getAnnotationName
    val rects = from match {
      case Annotations.From.BoundingRect => boundingRect(page, annotation)
      case Annotations.From.Quads        => quads(page, annotation)
    }
    rects.map(Mark(id, _))
  }

  private def boundingRect(
      page: PDPage,
      annotation: PDAnnotationTextMarkup
  ): Either[ProcessingPdfError, NonEmptyList[Rect2D]] = {
    Option(annotation.getRectangle)
      .map { rect =>
        val x = rect.getLowerLeftX
        val y = {
          val y = rect.getUpperRightY
          if (page.getRotation == 0) page.getMediaBox.getHeight - y
          else y
        }
        val w = rect.getWidth
        val h = rect.getHeight

        NonEmptyList.one(Rect2D.from(x = x, y = y, w = w, h = h))
      }
      .toRight(ProcessingPdfError("rectangle from annotation not found"))
  }

  private def quads(
      page: PDPage,
      annotation: PDAnnotationTextMarkup
  ): Either[ProcessingPdfError, NonEmptyList[Rect2D]] = {
    val quads = annotation.getQuadPoints

    (0 until quads.length / 8)
      .map { i =>
        var (minX, minY, maxX, maxY) = (Float.MaxValue, Float.MaxValue, Float.MinValue, Float.MinValue)

        for (j <- 0 until 8 by 2) {
          minX = Math.min(minX, quads(i * 8 + j))
          minY = Math.min(minY, quads(i * 8 + j + 1))
          maxX = Math.max(maxX, quads(i * 8 + j))
          maxY = Math.max(maxY, quads(i * 8 + j + 1))
        }

        val x = quads(i * 8) - 1
        val y = {
          val y = quads(i * 8 + 1) - 1
          if (page.getRotation == 0) page.getMediaBox.getHeight - y
          else y
        }
        val w = maxX - minX + 2
        val h = maxY - minY + 2

        Rect2D.from(x = x, y = y, w = w, h = h)
      }
      .toList
      .toNel
      .toRight(ProcessingPdfError("quads from annotation not found"))
  }

  implicit val orderingMark: Ordering[Mark] = (x: Mark, y: Mark) =>
    // compares by the text start position
    Ordering[Rect2D].compare(x.rects.sorted.head, y.rects.sorted.head)

}
