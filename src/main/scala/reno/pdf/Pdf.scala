package reno.pdf

import java.nio.file.Path

import cats.implicits._
import cats.effect.{Resource, Sync}
import io.chrisdavenport.log4cats.Logger
import org.apache.pdfbox.pdmodel.PDDocument

case class Pdf(info: PdfInfo, annotations: Annotations)

object Pdf {

  def fromPath[F[_]: Sync: Logger](path: Path, markFrom: Annotations.From = Annotations.From.Quads): F[Pdf] =
    Logger[F].info(s"Processing ${path.getFileName}") *>
      pdDocumentResource(path).use { doc =>
        val info = PdfInfo.fromPDDocument(doc, path)
        Annotations.extractAnnotations(doc, markFrom).map(Pdf(info, _))
      }

  private def pdDocumentResource[F[_]: Sync](path: Path): Resource[F, PDDocument] = {
    def acquire = Sync[F].delay(PDDocument.load(path.toFile))

    def release = (doc: PDDocument) => Sync[F].delay(doc.close())

    Resource.make(acquire)(release)
  }
}
