package reno

import java.awt.geom.Rectangle2D

import cats._

import scala.collection.LinearSeq

package object pdf {
  type Annotations = LinearSeq[Annotation]
}

package pdf {

  import java.nio.file.Path
  import java.util.Calendar

  import org.apache.pdfbox.pdmodel.PDDocument

  case class PdfInfo(title: String, author: String, subject: String, created: Calendar, keywords: String, path: Path)

  object PdfInfo {
    def fromPDDocument(doc: PDDocument, path: Path): PdfInfo = {
      val info = doc.getDocumentInformation
      PdfInfo(
        title = info.getTitle,
        author = info.getAuthor,
        subject = info.getSubject,
        created = info.getCreationDate,
        keywords = info.getKeywords,
        path = path
      )
    }
  }

  case class Rect2D(leftBottom: Pos, rightTop: Pos) {
    def toRectangle2D: Rectangle2D.Float =
      new Rectangle2D.Float(
        leftBottom.col,
        leftBottom.row,
        rightTop.col - leftBottom.col,
        rightTop.row - leftBottom.row
      )
  }

  object Rect2D {

    def from(x: Float, y: Float, w: Float, h: Float): Rect2D = {
      val (row, col) = (y, x)
      val leftBottom = Pos(row, col)
      val rightTop   = Pos(row + h, col + w)
      Rect2D(leftBottom, rightTop)
    }

    def fromRectangle2D(r: Rectangle2D.Float): Rect2D = {
      from(x = r.x, y = r.y, w = r.width, h = r.height)
    }

    implicit val orderingRect2D: Ordering[Rect2D] = (x: Rect2D, y: Rect2D) =>
      Ordering[Pos].compare(x.leftBottom, y.leftBottom)

    implicit val orderRect2D: Order[Rect2D] = Order.fromOrdering
  }

  /**
    * `Pos` represents a position in the pdf.
    */
  case class Pos(row: Float, col: Float)

  object Pos {
    implicit val orderingPos: Ordering[Pos] = (a: Pos, b: Pos) =>
      a.row compare b.row match {
        case 0 => a.col compare b.col
        case x => x
      }
  }

  case class ProcessingPdfError(msg: String) extends Throwable

}
