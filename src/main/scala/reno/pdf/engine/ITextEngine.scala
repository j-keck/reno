package reno.pdf.engine

import java.awt.geom.Rectangle2D
import java.nio.file.Path

import cats.effect.{Resource, Sync}
import cats.implicits._
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf._
import com.itextpdf.kernel.pdf.annot.PdfTextMarkupAnnotation
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import com.itextpdf.kernel.pdf.canvas.parser.filter.TextRegionEventFilter
import com.itextpdf.kernel.pdf.canvas.parser.listener.{FilteredTextEventListener, LocationTextExtractionStrategy}
import io.chrisdavenport.log4cats.Logger
import reno.pdf._
import reno.pdf.engine.PageInfo.Orientation

import scala.jdk.CollectionConverters._

object ITextEngine extends Engine[PdfDocument, PdfPage, PdfTextMarkupAnnotation] {

  override def docResource[F[_]: Sync: Logger](path: Path): Resource[F, PdfDocument] = {
    def acquire = Sync[F].delay(new PdfReader(path.toString))

    def release = (doc: PdfReader) => Sync[F].delay(doc.close())

    Resource.make(acquire)(release).map(new PdfDocument(_))
  }

  override def extractPdfInfo[F[_]: Sync: Logger](doc: PdfDocument, path: Path): F[PdfInfo] = {
    val info    = doc.getDocumentInfo
    val created = PdfDate.decode(info.getMoreInfo(PdfName.CreationDate.getValue))
    PdfInfo(
      title = info.getTitle,
      author = info.getAuthor,
      subject = info.getSubject,
      created,
      keywords = info.getKeywords,
      path
    ).pure[F]
  }

  override def extractAnnotations[F[_]: Sync: Logger](doc: PdfDocument, markFrom: Mark.From): F[Annotations] = {
    val pages: LazyList[(PdfPage, Int)] =
      LazyList.range(0, doc.getNumberOfPages).map(_ + 1).map(i => doc.getPage(i) -> i)

    pages
      .traverse {
        case (page, pageNumber) =>
          // extract annotations for each page.
          // skip currently not implemented annotations.
          val annotationsPerPage: F[List[Annotation]] = page.getAnnotations.asScala.toList.flatTraverse {
            case annotation: PdfTextMarkupAnnotation =>
              Logger[F].trace(s"TextMarkup on page $pageNumber found") *>
                Sync[F]
                  .fromEither(getMark(page, annotation, markFrom))
                  .flatMap { mark =>
                    // FIXME: `getContents` returns a empty `PdfString`
                    // use `null` to extract the text by position
                    //Option(annotation.getContents)
                    Option[PdfString](null)
                      .fold {
                        Logger[F].trace("Extract content by position") *> {
                          mark.rects
                            .traverse { rect2D =>
                              val itextRect    = new Rectangle(rect2D.x, rect2D.y, rect2D.w, rect2D.h)
                              val regionFilter = new TextRegionEventFilter(itextRect)
                              val strategy =
                                new FilteredTextEventListener(new LocationTextExtractionStrategy(), regionFilter)
                              PdfTextExtractor.getTextFromPage(page, strategy).pure[F]
                            }
                        }.map(_.toList.mkString)
                      } { pdfString =>
                        Logger[F].trace("Extract embedded content") *>
                          Option(pdfString.getEncoding).fold(pdfString.toUnicodeString)(_ => pdfString.getValue).pure[F]
                      }
                      .map(text => List(TextMarkupAnnotation(pageNumber, mark, text)))
                  }

            case annotation =>
              Logger[F].trace(s"Ignoring annotation: ${annotation.getClass.getName} on page $pageNumber") *>
                List.empty[Annotation].pure[F]

          }
          annotationsPerPage
      }
      .map(_.flatten.sorted)

  }

  override protected def getPageInfo(page: PdfPage): PageInfo = {
    val mb = page.getMediaBox
    val orientation = page.getRotation match {
      case 0 => Orientation.Landscape
      case _ => Orientation.Portrait
    }
    PageInfo(orientation, mb.getWidth, mb.getHeight)
  }

  override protected def getAnnotationId(annotation: PdfTextMarkupAnnotation): String = annotation.getName.getValue

  override protected def getBoundingRect(
      annotation: PdfTextMarkupAnnotation
  ): Either[ProcessingPdfError, Rectangle2D.Float] = {
    val pdRect = annotation.getRectangle
    val x      = pdRect.getAsNumber(0).floatValue
    val y      = pdRect.getAsNumber(1).floatValue
    val w      = pdRect.getAsNumber(2).floatValue - x
    val h      = pdRect.getAsNumber(3).floatValue - y
    new Rectangle2D.Float(x, y, w, h).asRight
  }

  override protected def getQuadPoints(annotation: PdfTextMarkupAnnotation): Either[ProcessingPdfError, Array[Float]] =
    annotation.getQuadPoints.toFloatArray.asRight

}
