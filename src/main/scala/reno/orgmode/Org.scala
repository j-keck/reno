package reno.orgmode

import java.io.FileWriter
import java.nio.file.Path

import cats.Applicative
import cats.effect.{Resource, Sync}
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import reno.orgmode.NoteType._
import reno.pdf.{Annotation, Pdf, TextMarkupAnnotation}

import scala.collection.LinearSeq
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
            case LatexFragment(id) =>
              s"""\\begin{$id}
                 |${note.text}
                 |\\end{$id}
                 |""".stripMargin
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
    def go(notes: Option[Zipper[Note]], annotations: Option[Zipper[Annotation]]): F[Notes] =
      (notes, annotations) match {
        // note without :reno_marker_id: reference - take it
        case (Some(ns), _) if ns.focus.ids.isEmpty =>
          Logger[F].debug(s"free text note (note-idx: ${ns.idx})") *>
            go(ns.next, annotations).map(ns.focus +: _)

        // note has a reference to the annotation - take it and proceed
        case (Some(ns), Some(as)) if ns.focus.ids.contains(as.focus.mark.id) =>
          Logger[F].debug(s"note from annotation (note-idx: ${ns.idx}, annotation-idx: ${as.idx})") *>
            go(ns.next, as.nextWhile(a => ns.focus.ids.contains(a.mark.id))).map(ns.focus +: _)

        // must be a new annotation - take it
        case (Some(ns), Some(as)) if !ns.exists(_.ids.contains(as.focus.mark.id)) =>
          Logger[F].debug(s"new annotation (annotation-idx: ${as.idx}") *>
            go(notes, as.next).map { rest =>
              val note = (as.focus match {
                case a: TextMarkupAnnotation => Note.quote(a.text, Seq(a.mark.id))
              })
              note +: rest
            }

        // annotation must be removed - remove the reference from the actual note
        case (Some(ns), _) =>
          Logger[F]
            .info(
              s"annotation removed - (note-idx: ${ns.idx})"
            ) *>
            go(ns.next, annotations).map(ns.focus.copy(ids = ns.focus.ids.tail) +: _)

        // all notes processed - continue with the remaining annotations
        case (None, Some(as)) =>
          Logger[F].debug(s"notes processed - new annotation (annotation-idx: ${as.idx}") *>
            go(notes, as.next).map { rest =>
              val note = (as.focus match {
                case a: TextMarkupAnnotation => Note.quote(a.text, Seq(a.mark.id))
              })
              note +: rest
            }

        // everything processed - done
        case (None, None) =>
          Logger[F].debug("everything processed") *> LinearSeq.empty.pure[F]

      }

    go(Zipper.fromLinearSeq(notes), Zipper.fromLinearSeq(pdf.annotations)).map(notes => copy(notes = notes))
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
