package reno.orgmode

import java.io.FileWriter
import java.nio.file.Path

import cats.Applicative
import cats.effect.{Resource, Sync}
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import reno.orgmode.NoteType.{Heading, Quote, Src, Text}
import reno.pdf.{Annotations, Pdf, TextMarkupAnnotation}

import scala.io.BufferedSource

case class Org(header: Header, notes: Notes) {

  def text: String = {
    header.map { case (name, value) => s"#+$name: $value" }.mkString("", "\n", "\n\n") +
      notes
        .map { note =>
          val text = note.tpe match {
            case Heading | Text => note.text
            case Quote          => s"#+BEGIN_QUOTE\n${note.text}\n#+END_QUOTE"
            case Src            => s"#+BEGIN_SRC\n${note.text}\n#+END_SRC"

          }

          val ids = if (note.ids.nonEmpty) note.ids.mkString(":RENO_MARKER_IDS:\n", "\n", "\n:END:\n") else ""

          s"$text\n$ids"
        }
        .mkString("\n", "\n", "\n")
  }

  def save[F[_]: Sync: Logger](path: Path): F[Unit] =
    Logger[F].info(s"Write $path") *>
      Resource.fromAutoCloseable(Sync[F].delay(new FileWriter(path.toFile))).use(fw => Sync[F].delay(fw.write(text)))

  def update[F[_]: Applicative: Logger](pdf: Pdf): F[Org] = {
    def go(notes: Notes, annotations: Annotations): F[Notes] =
      (notes, annotations) match {

        // same id on both sides - skip all merged annotations
        case (n :: ns, a :: as) if n.ids.contains(a.mark.id) =>
          Logger[F].debug(s"unchanged ids: ${n.ids.mkString(", ")}") *> go(
            ns,
            as.dropWhile(a => n.ids.contains(a.mark.id))
          ).map(n +: _)

        // note without :reno_marker_id: reference - go with the note
        case (n :: ns, as) if n.ids.isEmpty =>
          Logger[F].debug(s"keep free text: ${n.text.take(60).trim}...") *> go(ns, as).map(n +: _)

        // new annotation
        case (ns, a :: as) if !notes.flatMap(_.ids).contains(a.mark.id) =>
          Logger[F].debug(s"found new annotation with id: ${a.mark.id}") *>
            go(ns, as).map(
              // NOTE: this will change when more annotations are supported
              (a match {
                case a: TextMarkupAnnotation => Note.quote(a.text, Seq(a.mark.id))
              }) +: _
            )

        // annotation removed
        case (n :: ns, as) =>
          // `n.ids.head` is save, because notes without any id's are already processed
          Logger[F]
            .info(s"no reference to note '${n.ids.head}' found - keep the note but remove the attached id") *> go(
            ns,
            as
          ).map(n.copy(ids = n.ids.filterNot(_ == n.ids.head)) +: _)

        case (ns, Nil) => Logger[F].debug("all annotations processed") *> ns.pure[F]

      }

    go(notes, pdf.annotations).map(notes => copy(notes = notes))
  }
}

object Org {

  def fromPDF(pdf: Pdf): Org =
    Org(
      List("TITLE" -> pdf.info.title, "OPTIONS" -> "d:nil"),
      pdf.annotations.map {
        case a: TextMarkupAnnotation => Note.quote(a.text, Seq(a.mark.id))
      }
    )

  def fromOrg[F[_]: Sync: Logger](path: Path): F[Org] = {
    val acquire = Sync[F].delay(scala.io.Source.fromFile(path.toFile))
    val release = (bs: BufferedSource) => Sync[F].delay(bs.close)

    Logger[F].info(s"Load org-file $path") *>
      Resource
        .make[F, BufferedSource](acquire)(release)
        .use { bs =>
          val lines = bs.getLines().toList
          Parser.parse[F](lines).flatMap(Sync[F].fromEither(_))
        }
  }
}
