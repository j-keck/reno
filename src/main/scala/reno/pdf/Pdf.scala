package reno.pdf

import java.nio.file.Path

import cats.effect.Sync
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import reno.pdf.engine.{ITextEngine, PDFBoxEngine}

case class Pdf(info: PdfInfo, annotations: Annotations)

object Pdf {

  def fromPath[F[_]: Sync: Logger](
      path: Path,
      engine: PdfEngine,
      markFrom: Mark.From
  ): F[Pdf] =
    Logger[F].info(s"Processing ${path.getFileName}") *>
      (engine match {
        case PdfEngine.PDFBox => PDFBoxEngine.fromPath(path, markFrom)
        case PdfEngine.IText  => ITextEngine.fromPath(path, markFrom)
      })

}
