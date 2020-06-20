package reno.pdf

import cats.effect._
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import org.apache.pdfbox.pdmodel.interactive.annotation._
import org.apache.pdfbox.pdmodel.{PDDocument, PDPage}
import org.apache.pdfbox.text._

import scala.jdk.CollectionConverters._

object Annotations {

  sealed trait From

  object From {

    case object BoundingRect extends From

    case object Quads extends From

  }

  def extractAnnotations[F[_]: Sync: Logger](
      doc: PDDocument,
      markFrom: From
  ): F[Annotations] = {
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
                    Mark
                      .from(page, annotation, markFrom)
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

  // list annotation PDFBox classes
  def listAnnotations[F[_]: Sync](doc: PDDocument): F[Seq[String]] =
    Sync[F].delay(doc.getDocumentCatalog.getPages.asScala.flatMap { page =>
      page.getAnnotations.asScala.toList.map(_.getClass.getName)
    }.toSeq)

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

    // build the resulting text
    val sentence =
      for (id <- stripper.getRegions.asScala)
        yield stripper.getTextForRegion(id)
    sentence.mkString(" ").replaceAll("\n", " ").replaceAll(" {2}", " ")
  }
}
