package reno

import java.nio.file.Path

import cats.effect._
import cats.implicits._
import com.monovore.decline._
import com.monovore.decline.effect._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import reno.orgmode.Org
import reno.pdf.{Pdf, TextMarkupAnnotation}

object Reno
    extends CommandIOApp(
      name = "reno",
      header = "reading notes extractor",
      version = reno.BuildInfo.version
    ) {

  implicit def unsafeLogger[F[_]: Sync] = Slf4jLogger.getLogger[F]

  override def main: Opts[IO[ExitCode]] =
    (dumpOpts orElse updateOpts orElse debugShowOpts)
      .map {
        case opts: Dump      => dump[IO](opts)
        case opts: Update    => update[IO](opts)
        case opts: DebugShow => debugShow[IO](opts)
      }
      .map(_.handleErrorWith {
        case e => IO(System.err.println(RenoError.format(e)))
      }
        .as(ExitCode.Success))

  /**
    * dump the annotations from the given pdf
    */
  def dump[F[_]: Sync](opts: Dump): F[Unit] = {
    val orgIsNewOrCanOverwritten: F[Unit] =
      if (opts.org.toFile.exists() && !opts.overwriteExisting)
        Sync[F].raiseError(InvalidArgumentError(s"target file exists - use '-f' to override it"))
      else Sync[F].unit

    for {
      _   <- canWrite(opts.org)
      _   <- orgIsNewOrCanOverwritten
      pdf <- Pdf.fromPath(opts.pdf, opts.pdfEngine, opts.markFrom)
      org = Org.fromPDF(pdf)
      _ <- org.save(opts.org)
    } yield ()
  }

  /**
    * update the reading notes from the given pdf
    */
  def update[F[_]: Sync](opts: Update): F[Unit] =
    for {
      _       <- canWrite(opts.dstOrg)
      pdf     <- Pdf.fromPath(opts.pdf, opts.pdfEngine, opts.markFrom)
      org     <- Org.fromOrg(opts.srcOrg)
      updated <- org.update[F](pdf)
      _       <- updated.save(opts.dstOrg)
    } yield ()

  def debugShow[F[_]: Sync](opts: DebugShow): F[Unit] =
    for {
      pdf <- Pdf.fromPath(opts.pdf, opts.pdfEngine, opts.markFrom)

      _ <- pdf.annotations.toList.traverse_ {
        case TextMarkupAnnotation(pageNumber, mark, text) =>
          Sync[F].delay {
            println(s"Site: ${pageNumber} (${mark.id}@${mark.startPos})}")
            println(s"$text\n")
          }
      }
    } yield ()

  /**
    * checks if the given path is writable
    */
  private def canWrite[F[_]: Sync](path: Path): F[Unit] = {
    val file             = path.toFile
    def err(msg: String) = Sync[F].raiseError[Unit](InvalidArgumentError(s"path '$file' $msg"))

    if (file.isDirectory) err("is a directory")
    else if (file.exists() && !file.canWrite) err("is not writable")
    else Sync[F].unit
  }
}
