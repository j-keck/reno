import java.nio.file.{Path, Paths}

import cats.syntax.apply._
import com.monovore.decline._

package object reno {

  val dumpOpts: Opts[Dump] =
    Opts
      .subcommand("dump", "Dump the annotations from the given Pdf") {
        (
          Opts.flag(long = "overwrite", help = "Overwrite existing file", "f").orFalse,
          Opts.argument[Path](metavar = "pdf"),
          Opts.argument[Path](metavar = "org").orNone
        ).mapN {
          case (overwiteExisting, pdf, org) =>
            Dump(pdf, org.getOrElse(mkDefaultOrgPath(pdf)), overwiteExisting)
        }
      }

  val updateOpts: Opts[Update] =
    Opts.subcommand("update", "update the notes in the Org-file with new annotations from the Pdf") {
      (
        Opts.argument[Path](metavar = "pdf"),
        Opts.argument[Path](metavar = "src-org").orNone,
        Opts.argument[Path](metavar = "dst-org").orNone
      ).mapN {
        case (pdf, srcOrg, dstOrg) =>
          val defaultOrg = mkDefaultOrgPath(pdf)
          Update(pdf, srcOrg.getOrElse(defaultOrg), dstOrg.getOrElse(defaultOrg))
      }
    }

  private def mkDefaultOrgPath(p: Path): Path =
    Paths.get(p.getFileName.toString.split('.') match {
      case Array(single) => single + "-notes.org"
      case xs            => xs.init.mkString(".") + "-notes.org"
    })
}

package reno {

  case class Dump(pdf: Path, org: Path, overwriteExisting: Boolean)

  case class Update(pdf: Path, srcOrg: Path, dstOrg: Path)

  abstract class RenoError(msg: String) extends Throwable(msg) {
    override def toString: String =
      s"${getClass.getSimpleName} - ${msg}\n${humanReadableCallStack(2)}"

    def humanReadableCallStack(indent: Int = 0): String = {
      val rootPackageName = getClass.getPackage.getName.takeWhile(_ != '.')
      super.getStackTrace
        .takeWhile(_.getClassName.startsWith(rootPackageName))
        .map(s => s".${s.getMethodName}(${s.getFileName}:${s.getLineNumber})")
        .mkString(s"${" " * indent}\u2620 ", s"\n${" " * indent}\u21E7 ", "\n")
    }
  }

  case class InvalidArgumentError(msg: String) extends RenoError(msg)

}
