package reno.pdf.engine

import java.awt.geom.Rectangle2D
import java.nio.file.Path

import cats.effect.{Resource, Sync}
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup
import org.apache.pdfbox.pdmodel.{PDDocument, PDPage}
import org.apache.pdfbox.text.PDFTextStripperByArea
import reno.pdf._
import reno.pdf.engine.PageInfo.Orientation

import scala.jdk.CollectionConverters._

object PDFBoxEngine extends Engine[PDDocument, PDPage, PDAnnotationTextMarkup] {

  override def docResource[F[_]: Sync: Logger](path: Path): Resource[F, PDDocument] = {
    def acquire = Sync[F].delay(PDDocument.load(path.toFile))

    def release = (doc: PDDocument) => Sync[F].delay(doc.close())

    Resource.make(acquire)(release)
  }

  override def extractPdfInfo[F[_]: Sync: Logger](doc: PDDocument, path: Path): F[PdfInfo] = {
    val info = doc.getDocumentInformation
    PdfInfo(
      title = info.getTitle,
      author = info.getAuthor,
      subject = info.getSubject,
      created = info.getCreationDate,
      keywords = info.getKeywords,
      path = path
    ).pure[F]
  }

  override def extractAnnotations[F[_]: Sync: Logger](doc: PDDocument, markFrom: Mark.From): F[Annotations] = {
    val pages: List[(PDPage, Int)] =
      doc.getDocumentCatalog.getPages.asScala.zip(LazyList.from(1)).toList

    pages
      .traverse {
        case (page, pageNumber) =>
          // extract annotations for each page.
          // skip currently not implemented annotations.
          val annotationsPerPage: F[List[Annotation]] = page.getAnnotations.asScala.toList.flatTraverse {
            case annotation: PDAnnotationTextMarkup =>
              Logger[F].trace(s"TextMarkup on page $pageNumber found") *>
                Sync[F]
                  .fromEither[Annotation](
                    getMark(page, annotation, markFrom)
                      .map { mark =>
                        val text = extractText(page, mark)
                        TextMarkupAnnotation(pageNumber, mark, text)
                      }
                  )
                  .map(List.apply(_))

            case annotation =>
              Logger[F].trace(s"Ignoring annotation: ${annotation.getClass.getName} on page $pageNumber") *>
                List.empty[Annotation].pure[F]
          }

          // sort the annotations by position.
          // when extracting the annotations from the pdf, the order depends
          // on the sequence when the annotation was made.
          annotationsPerPage.map(_.sorted)

      }
      .map(_.flatten.toSeq)
  }

  private def extractText(page: PDPage, mark: Mark): String = {
    val stripper = new PDFTextStripperByArea()

    // i don't not why, but the stripper doesn't sort the text by position.
    // sort the result later by hand.
    //stripper.setSortByPosition(true)

    // register all regions to extract
    mark.rects.zipWithIndex.map {
      case (rect, id) =>
        stripper.addRegion(id.toString, rect.toRectangle2D)
    }

    // run the extraction
    stripper.extractRegions(page)

    // build the resulting textDoc
    val sentence =
      for (id <- stripper.getRegions.asScala)
        yield stripper.getTextForRegion(id)
    sentence.mkString(" ").replaceAll("\n", " ").replaceAll(" {2}", " ")
  }

  override protected def getPageInfo(page: PDPage): PageInfo = {
    val mb = page.getMediaBox
    val orientation = page.getRotation match {
      case 0 => Orientation.Portrait
      case _ => Orientation.Landscape
    }
    PageInfo(orientation, mb.getWidth, mb.getHeight)
  }

  override protected def getAnnotationId(annotation: PDAnnotationTextMarkup): String = annotation.getAnnotationName

  override protected def getBoundingRect(
      annotation: PDAnnotationTextMarkup
  ): Either[ProcessingPdfError, Rectangle2D.Float] = {
    val pdRect = annotation.getRectangle
    new Rectangle2D.Float(pdRect.getLowerLeftX, pdRect.getUpperRightY, pdRect.getWidth, pdRect.getHeight).asRight
  }

  override protected def getQuadPoints(annotation: PDAnnotationTextMarkup): Either[ProcessingPdfError, Array[Float]] =
    annotation.getQuadPoints.asRight

}
