package reno.orgmode

import cats.{Applicative, Monad}
import cats.data.EitherT
import cats.implicits._
import io.chrisdavenport.log4cats.Logger

import scala.collection.LinearSeq

object Parser {

  type ParseResult[A] = Either[ParserError, A]

  def parse[F[_]: Monad: Logger](iterableOnce: IterableOnce[String]): F[ParseResult[Org]] = {
    val xs = iterableOnce.iterator.to(List)
    (for {
      (header, xs) <- EitherT(parseHeader[F](xs))
      (notes, _)   <- EitherT(parseNotes[F](xs))
    } yield Org(header, notes)).value
  }

  private def parseHeader[F[_]: Applicative](xs: List[String]): F[ParseResult[(Header, List[String])]] = {
    def go(xs: List[String]): ParseResult[(Header, List[String])] =
      xs.headOption match {
        // FIXME: this is very hacky and unsafe!
        case Some(line) if hasOrgMarker(line, "#+") && line.contains(":") =>
          val Array(key, value) = line.split(":", 2)
          go(xs.tail).map {
            case (headers, xs) =>
              ((key.replaceFirst("\\s*#\\+\\s*", "") -> value.trim) +: headers, xs)
          }
        case _ => (Nil, xs).asRight
      }

    go(xs).pure[F]
  }

  private def parseNotes[F[_]: Applicative](xs: List[String]): F[ParseResult[(Notes, List[String])]] = {

    def go(xs: List[String]): ParseResult[(Notes, List[String])] = {
      xs.headOption match {

        // skip blank lines
        case Some(line) if line.trim.isEmpty => go(xs.tail)

        // header
        case Some(line) if hasOrgMarker(line, "*") =>
          for {
            (ids, xs)  <- takeIds(xs.tail)
            (rest, xs) <- go(xs)
          } yield (Note.heading(line, ids) +: rest, xs)

        // quote blocks
        case Some(line) if hasOrgMarker(line, "#+begin_quote") =>
          for {
            (quote, xs) <- takeBlock(xs, "#+begin_quote", "#+end_quote")
            (ids, xs)   <- takeIds(xs)
            (rest, xs)  <- go(xs)
          } yield (Note.quote(quote.mkString("\n"), ids) +: rest, xs)

        // source blocks
        case Some(line) if hasOrgMarker(line, "#+begin_src") =>
          for {
            (src, xs)  <- takeBlock(xs, "#+begin_src", "#+end_src")
            (ids, xs)  <- takeIds(xs)
            (rest, xs) <- go(xs)
          } yield (Note.src(src.mkString("\n"), ids) +: rest, xs)

        // latex fragment
        case Some(line) if hasOrgMarker(line, "\\begin{") =>
          val id = line.dropWhile(_ != '{').tail.takeWhile(_ != '}')
          for {
            (fragment, xs) <- takeBlock(xs, s"\\begin{$id}", s"\\end{$id}")
            (ids, xs)      <- takeIds(xs)
            (rest, xs)     <- go(xs)
          } yield (Note.latexFragment(id, fragment.mkString("\n"), ids) +: rest, xs)

        // text
        case Some(_) =>
          for {
            (text, xs) <- takeText(xs)
            (ids, xs)  <- takeIds(xs)
            (rest, xs) <- go(xs)
          } yield (Note.text(text.mkString("\n"), ids) +: rest, xs)

        case None => (LinearSeq.empty, xs).asRight
      }
    }

    go(xs).pure[F]
  }

  private def hasOrgMarker(s: String, prefix: String) = s.trim.toLowerCase.startsWith(prefix.toLowerCase())

  private[reno] def takeBlock(
      xs: List[String],
      start: String,
      end: String
  ): ParseResult[(Seq[String], List[String])] = {
    def go(xs: List[String]): ParseResult[(Seq[String], List[String])] =
      xs.headOption match {
        case Some(line) if hasOrgMarker(line, end) => (Seq.empty[String], xs.tail).asRight
        case Some(line)                            => go(xs.tail).map { case (rest, xs) => (line +: rest, xs) }
        case None                                  => ParserError(s"End marker: '$end' not found").asLeft
      }

    if (xs.headOption.map(_.equalsIgnoreCase(start)).getOrElse(false))
      go(xs.tail)
    else
      ParserError(s"Start marker: '$start' not found - headOption: ${xs.headOption}").asLeft
  }

  private[reno] def takeIds(origList: List[String]): ParseResult[(Seq[String], List[String])] = {
    def go(xs: List[String]): (Seq[String], List[String]) =
      xs.headOption match {
        // skip empty lines
        case Some(line) if line.trim.isEmpty => go(xs.tail)

        // check if there is a drawer with mark id's
        case Some(line) if hasOrgMarker(line, ":reno_marker_ids:") =>
          // take all lines until the end mark
          val ids = xs.tail.takeWhile(!hasOrgMarker(_, ":end:"))
          (ids.to(Seq), xs.dropWhile(!hasOrgMarker(_, ":end")).tail)

        case _ =>
          // no drawer with mark id's found - return the original List
          (Seq.empty, origList)
      }

    go(origList).asRight
  }

  private def takeText(xs: List[String]): ParseResult[(Seq[String], List[String])] = {
    def go(xs: List[String]): ParseResult[(Seq[String], List[String])] = {
      xs.headOption match {
        case Some(line) if Seq("*", "#+begin_quote", "#+begin_src", "\\begin{").exists(hasOrgMarker(line, _)) =>
          // use the full List to keep the marker line intact
          (Seq.empty, xs).asRight
        case Some(line) =>
          go(xs.tail).map {
            case (lines, xs) =>
              (line +: lines, xs)
          }
        case None => (Seq.empty, xs).asRight
      }
    }

    go(xs)
  }

}
