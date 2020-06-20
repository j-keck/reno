package reno.orgmode

import cats.{Applicative, Monad}
import cats.data.EitherT
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import reno.RenoError

import scala.collection.LinearSeq

object Parser {

  type ParseResult[A] = Either[ParserError, A]

  case class ParserError(msg: String) extends RenoError(msg)

  def parse[F[_]: Monad: Logger](iterableOnce: IterableOnce[String]): F[ParseResult[Org]] = {
    val iter = SavepointIterator.fromIterableOnce(iterableOnce)
    (for {
      header <- EitherT(parseHeader[F](iter))
      notes  <- EitherT(parseNotes[F](iter))
    } yield Org(header, notes)).value
  }

  private def parseHeader[F[_]: Applicative](iter: SavepointIterator[String]): F[ParseResult[Header]] = {
    def go(iter: SavepointIterator[String]): ParseResult[Header] =
      iter.nextOption() match {
        // FIXME: this is very hacky and unsafe!
        case Some(line) if hasOrgMarker(line, "#+") && line.contains(":") =>
          iter.savepoint()
          val Array(key, value) = line.split(":", 2)
          go(iter).map((key.replaceFirst("\\s*#\\+\\s*", "") -> value.trim) +: _)
        case _ =>
          iter.rollback()
          Nil.asRight
      }

    iter.savepoint()
    go(iter).pure[F]
  }

  private def parseNotes[F[_]: Applicative](iter: SavepointIterator[String]): F[ParseResult[Notes]] = {

    def go(iter: SavepointIterator[String]): ParseResult[Notes] = {
      iter.nextOption() match {

        // skip blank lines
        case Some(line) if line.trim.isEmpty => go(iter)

        // header
        case Some(line) if hasOrgMarker(line, "*") =>
          for {
            ids  <- takeIds(iter)
            rest <- go(iter)
          } yield Note.heading(line, ids) +: rest

        // quote blocks
        case Some(line) if hasOrgMarker(line, "#+begin_quote") =>
          for {
            quote <- takeBlock(iter, "#+end_quote")
            ids   <- takeIds(iter)
            rest  <- go(iter)
          } yield Note.quote(quote, ids) +: rest

        // source blocks
        case Some(line) if hasOrgMarker(line, "#+begin_src") =>
          for {
            src  <- takeBlock(iter, "#+end_src")
            ids  <- takeIds(iter)
            rest <- go(iter)
          } yield Note.src(src, ids) +: rest

        // text
        case Some(line) =>
          for {
            text <- takeText(iter)
            ids  <- takeIds(iter)
            rest <- go(iter)
          } yield Note.text(line + "\n" + text, ids) +: rest

        case None => LinearSeq.empty.asRight
      }
    }

    go(iter).pure[F]
  }

  private def hasOrgMarker(s: String, prefix: String) = s.trim.toLowerCase.startsWith(prefix.toLowerCase())

  private def takeBlock(iter: SavepointIterator[String], marker: String): ParseResult[String] = {
    def go(iter: SavepointIterator[String]): ParseResult[Seq[String]] =
      iter.nextOption() match {
        case Some(line) if hasOrgMarker(line, marker) => Seq.empty[String].asRight
        case Some(line)                               => go(iter).map(line +: _)
        case None                                     => ParserError(s"End marker: '$marker' not found").asLeft[Seq[String]]
      }

    go(iter).map(_.mkString("\n"))
  }

  private def takeIds(iter: SavepointIterator[String]): ParseResult[Seq[String]] = {
    @scala.annotation.tailrec
    def go(iter: SavepointIterator[String]): Seq[String] =
      iter.nextOption() match {
        // skip empty lines
        case Some(line) if line.trim.isEmpty => go(iter)

        // check if there is a drawer with mark id's
        case Some(line) if hasOrgMarker(line, ":reno_marker_ids:") =>
          // take all lines until the end mark
          iter.iterator.takeWhile(!hasOrgMarker(_, ":end:")).to(Seq)

        case _ =>
          // no drawer with mark id's found - rollback the iterator
          iter.rollback()
          Seq.empty
      }

    iter.savepoint()
    go(iter).asRight
  }

  private def takeText(iter: SavepointIterator[String]): ParseResult[String] = {
    def go(iter: SavepointIterator[String]): ParseResult[Seq[String]] = {
      iter.savepoint()
      iter.nextOption() match {
        case Some(line) if Seq("*", "#+begin_quote", "#+begin_src").exists(hasOrgMarker(line, _)) =>
          // rollback to the savepoint to keep the marker line intact
          iter.rollback()
          Seq.empty[String].asRight
        case Some(line) =>
          go(iter).map(line +: _)
        case None =>
          Seq.empty[String].asRight
      }
    }

    go(iter).map(_.mkString("\n"))
  }

}
