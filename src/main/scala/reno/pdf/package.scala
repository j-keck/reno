package reno

import java.awt.geom.Rectangle2D
import java.nio.file.Path
import java.util.Calendar

import cats._

import scala.collection.LinearSeq

package object pdf {
  type Annotations = LinearSeq[Annotation]
}

package pdf {

  import _root_.enumeratum._

  sealed trait PdfEngine extends EnumEntry with EnumEntry.Lowercase
  object PdfEngine extends Enum[PdfEngine] {
    val values = findValues

    case object PDFBox extends PdfEngine
    case object IText  extends PdfEngine
  }

  case class PdfInfo(title: String, author: String, subject: String, created: Calendar, keywords: String, path: Path)

  case class Rect2D(leftBottom: Pos, rightTop: Pos) {
    lazy val x = leftBottom.x
    lazy val y = leftBottom.y
    lazy val w = rightTop.x - leftBottom.x
    lazy val h = rightTop.y - leftBottom.y

    def toRectangle2D: Rectangle2D.Float = new Rectangle2D.Float(x, y, w, h)
  }

  object Rect2D {

    def fromRectangle2D(r: Rectangle2D.Float): Rect2D = {
      from(x = r.x, y = r.y, w = r.width, h = r.height)
    }

    def from(x: Float, y: Float, w: Float, h: Float): Rect2D = {
      val leftBottom = Pos(x, y)
      val rightTop   = Pos(x + w, y + h)
      Rect2D(leftBottom, rightTop)
    }

    implicit val orderingRect2D: Ordering[Rect2D] = (x: Rect2D, y: Rect2D) =>
      Ordering[Pos].compare(x.leftBottom, y.leftBottom)

    implicit val orderRect2D: Order[Rect2D] = Order.fromOrdering
  }

  /**
    * `Pos` represents a position in the pdf.
    */
  case class Pos(x: Float, y: Float)

  object Pos {
    implicit val orderingPos: Ordering[Pos] = (a: Pos, b: Pos) =>
      a.y compare b.y match {
        case 0 => a.x compare b.x
        case x => x
      }
  }

  case class ProcessingPdfError(msg: String) extends Throwable

}
