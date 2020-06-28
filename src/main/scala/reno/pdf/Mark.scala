package reno.pdf

import java.awt.geom.Rectangle2D

import cats.data.NonEmptyList
import cats.implicits._
import reno.pdf.engine.PageInfo
import reno.pdf.engine.PageInfo.Orientation

/**
  * Marked area in the pdf
  */
case class Mark private[reno] (id: String, rects: NonEmptyList[Rect2D]) {

  def onSameLines(other: Mark): Boolean = {
    val rows = this.rects.flatMap(r => NonEmptyList.of(r.leftBottom.y, r.rightTop.y))
    other.rects.exists(rect => rows.exists(y => y == rect.leftBottom.y || y == rect.rightTop.y))
  }

  def startPos: Rect2D = rects.sorted.head

}

object Mark {

  import _root_.enumeratum._

  def fromBoundingRect(id: String, pageInfo: PageInfo, rect: Rectangle2D.Float): Either[ProcessingPdfError, Mark] = {
    if (pageInfo.orientation == Orientation.Portrait) rect.y = pageInfo.height - rect.y
    Mark(id, NonEmptyList.one(Rect2D.fromRectangle2D(rect))).asRight
  }

  def fromQuadPoints(
      id: String,
      pageInfo: PageInfo,
      quads: Array[Float]
  ): Either[ProcessingPdfError, Mark] =
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
          if (pageInfo.orientation == Orientation.Portrait) pageInfo.height - y
          else y
        }
        val w = maxX - minX + 2
        val h = maxY - minY + 2

        Rect2D.from(x = x, y = y, w = w, h = h)
      }
      .toList
      .toNel
      .toRight(ProcessingPdfError("quads from annotation not found"))
      .map(Mark(id, _))

  sealed trait From extends EnumEntry with EnumEntry.Lowercase

  object From extends Enum[From] {
    val values = findValues
    case object BoundingRect extends From
    case object Quads        extends From
  }

  implicit val orderingMark: Ordering[Mark] = (x: Mark, y: Mark) =>
    // compares by the text start position
    Ordering[Rect2D].compare(x.startPos, y.startPos)

}
