package reno.pdf

import java.nio.file.Paths

import cats.effect.{IO, Sync}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.scalatest.funsuite.AnyFunSuite
import reno.pdf.Mark.From.{BoundingRect, Quads}
import reno.pdf.PdfEngine.{IText, PDFBox}

// FIXME: landscape doesn't work
class PdfSuite extends AnyFunSuite {
  implicit def unsafeLogger[F[_]: Sync] = Slf4jLogger.getLogger[F]

  val path = Option(getClass.getClassLoader.getResource("example-marked.pdf"))
    .map(url => Paths.get(url.toURI))
    .getOrElse(fail("pdf not found"))

  ignore("PDFBox / BoundingRect") {
    assert(markedText(PDFBox, BoundingRect) === List("portrait", "landscape"))
  }

  ignore("PDFBox / Quads") {
    assert(markedText(PDFBox, Quads) === List("portrait", "landscape"))
  }

  ignore("iText / BoundingRect") {
    assert(markedText(IText, BoundingRect) === List("portrait", "landscape"))
  }

  ignore("iText / Quads") {
    assert(markedText(IText, Quads) === List("portrait", "landscape"))
  }

  def markedText(engine: PdfEngine, markFrom: Mark.From): List[String] = {
    val pdf = Pdf.fromPath[IO](path, engine, markFrom).unsafeRunSync()
    pdf.annotations.map {
      case a: TextMarkupAnnotation => a.text.trim
    }.toList
  }
}
