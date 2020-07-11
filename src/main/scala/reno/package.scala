import java.nio.file.{Path, Paths}

import cats.syntax.apply._
import com.monovore.decline._
import com.monovore.decline.enumeratum._
import reno.pdf.{Mark, PdfEngine}

package object reno {

  val exportOpts: Opts[Export] =
    Opts
      .subcommand("export", "Export the annotations from the given Pdf") {
        (
          Opts.flag(long = "overwrite", help = "Overwrite existing file", "f").orFalse,
          Opts
            .option[PdfEngine](long = "pdf-engine", s"Used pdf engine (itext / pdfbox)")
            .withDefault(PdfEngine.IText),
          Opts
            .option[Mark.From](long = "mark-from", s"Extract mark from (boundingrect / quads)")
            .withDefault(Mark.From.BoundingRect),
          Opts.argument[Path](metavar = "pdf"),
          Opts.argument[Path](metavar = "org").orNone
        ).mapN {
          case (overwiteExisting, pdfEngine, markFrom, pdf, org) =>
            Export(pdf, org.getOrElse(withBasenameSuffix(pdf, "-notes.org")), overwiteExisting, pdfEngine, markFrom)
        }
      }

  val updateOpts: Opts[Update] =
    Opts.subcommand("update", "update the notes in the Org-file with new annotations from the Pdf") {
      (
        Opts
          .option[PdfEngine](long = "pdf-engine", s"Used pdf engine (itext / pdfbox)")
          .withDefault(PdfEngine.IText),
        Opts
          .option[Mark.From](long = "mark-from", s"Extract mark from (boundingrect / quads)")
          .withDefault(Mark.From.BoundingRect),
        Opts.argument[Path](metavar = "pdf"),
        Opts.argument[Path](metavar = "src-org").orNone,
        Opts.argument[Path](metavar = "dst-org").orNone
      ).mapN {
        case (pdfEngine, markFrom, pdf, srcOrg, dstOrg) =>
          val defaultOrg = withBasenameSuffix(pdf, "-notes.org")
          Update(pdf, srcOrg.getOrElse(defaultOrg), dstOrg.getOrElse(defaultOrg), pdfEngine, markFrom)
      }
    }

  val debugShowOpts: Opts[DebugShow] =
    Opts.subcommand("debug-show", "debug show each annotation with meta informations") {
      (
        Opts
          .option[PdfEngine](long = "pdf-engine", s"Used pdf engine (itext / pdfbox)")
          .withDefault(PdfEngine.IText),
        Opts
          .option[Mark.From](long = "mark-from", s"Extract mark from (boundingrect / quads)")
          .withDefault(Mark.From.BoundingRect),
        Opts.argument[Path](metavar = "pdf")
      ).mapN {
        case (pdfEngine, markFrom, pdf) =>
          DebugShow(pdf, pdfEngine, markFrom)
      }
    }

  private def withBasenameSuffix(p: Path, suffix: String): Path =
    Paths.get(p.getFileName.toString.split('.') match {
      case Array(single) => single + suffix
      case xs            => xs.init.mkString(".") + suffix
    })
}

package reno {

  import reno.pdf.Mark

  case class Export(pdf: Path, org: Path, overwriteExisting: Boolean, pdfEngine: PdfEngine, markFrom: Mark.From)

  case class Update(pdf: Path, srcOrg: Path, dstOrg: Path, pdfEngine: PdfEngine, markFrom: Mark.From)

  case class DebugShow(pdf: Path, pdfEngine: PdfEngine, markFrom: Mark.From)

  case class DebugMarker(srcPdf: Path, dstPdf: Path, pdfEngine: PdfEngine, markFrom: Mark.From)

  abstract class RenoError(msg: String) extends Throwable(msg)

  object RenoError {
    def format(t: Throwable): String =
      s"${t.getClass.getSimpleName} - ${t.getMessage}\n${humanReadableCallStack(t, 2)}"

    def humanReadableCallStack(t: Throwable, indent: Int = 0): String = {
      def formatStackTrace(trace: Array[StackTraceElement]) =
        trace
          .map(s => s".${s.getMethodName}(${s.getFileName}:${s.getLineNumber})")
          .mkString(s"${" " * indent}\u2620 ", s"\n${" " * indent}\u21E7 ", "\n")

      val rootPackageName = getClass.getPackage.getName.takeWhile(_ != '.')

      formatStackTrace(
        t.getStackTrace
          .filter(_.getClassName.startsWith(rootPackageName))
      )
    }
  }

  case class InvalidArgumentError(msg: String) extends RenoError(msg)

}
