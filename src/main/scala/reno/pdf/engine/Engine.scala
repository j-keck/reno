package reno.pdf.engine

import java.awt.geom.Rectangle2D
import java.nio.file.Path
import cats.implicits._
import cats.effect.{Resource, Sync}
import io.chrisdavenport.log4cats.Logger
import reno.pdf._

trait Engine[PdfDoc, PdfPage, PdfAnnot] {

  def fromPath[F[_]: Sync: Logger](
      path: Path,
      markFrom: Mark.From
  ): F[Pdf] =
    docResource[F](path).use { doc =>
      for {
        info        <- extractPdfInfo(doc, path)
        annotations <- extractAnnotations(doc, markFrom)
      } yield Pdf(info, annotations)
    }

  def docResource[F[_]: Sync: Logger](path: Path): Resource[F, PdfDoc]

  def extractPdfInfo[F[_]: Sync: Logger](
      doc: PdfDoc,
      path: Path
  ): F[PdfInfo]

  def extractAnnotations[F[_]: Sync: Logger](
      doc: PdfDoc,
      markFrom: Mark.From
  ): F[Annotations]

  protected def getMark(page: PdfPage, annotation: PdfAnnot, markFrom: Mark.From): Either[ProcessingPdfError, Mark] = {
    val id       = getAnnotationId(annotation)
    val pageInfo = getPageInfo(page)
    markFrom match {
      case Mark.From.BoundingRect => getBoundingRect(annotation).flatMap(Mark.fromBoundingRect(id, pageInfo, _))
      case Mark.From.Quads        => getQuadPoints(annotation).flatMap(Mark.fromQuadPoints(id, pageInfo, _))
    }
  }

  protected def getPageInfo(page: PdfPage): PageInfo
  protected def getAnnotationId(annotation: PdfAnnot): String
  protected def getBoundingRect(annotation: PdfAnnot): Either[ProcessingPdfError, Rectangle2D.Float]
  protected def getQuadPoints(annotation: PdfAnnot): Either[ProcessingPdfError, Array[Float]]
}
