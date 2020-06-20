package reno.orgmode

import cats.data.{Chain, Writer}
import cats.syntax.either._
import io.chrisdavenport.log4cats.extras.{LogMessage, WriterLogger}
import org.scalatest.funsuite.AnyFunSuite
import reno.orgmode.Parser.ParserError

import scala.collection.LinearSeq

class ParserSuite extends AnyFunSuite {
  type PureLogger[A] = Writer[Chain[LogMessage], A]
  implicit def pureLogger = WriterLogger[Chain]()

  test("parse valid org-file") {
    val lines =
      """#+TITLE: Test
      |
      |* Title with ids
      |:reno_marker_ids:
      |id-001
      |:end:
      |
      | ** title without ids
      |
      |plaintext
      |over
      |some
      |lines
      |
      |#+begin_quote
      |quote without ids
      |#+end_quote
      |
      |#+begin_quote
      |quote with ids
      |#+end_quote
      |
      |:reno_marker_ids:
      |id-002
      |:end:
      |
      |#+begin_src
      |source with ids
      |#+end_src
      |
      |
      |:reno_marker_ids:
      |id-003
      |id-004
      |:end:
      |""".stripMargin.linesIterator

    val expected = Org(
      List("TITLE" -> "Test"),
      LinearSeq(
        Note.heading("* Title with ids", Seq("id-001")),
        Note.heading(" ** title without ids"),
        Note.text("plaintext\nover\nsome\nlines\n"),
        Note.quote("quote without ids"),
        Note.quote("quote with ids", Seq("id-002")),
        Note.src("source with ids", Seq("id-003", "id-004"))
      )
    ).asRight

    assert(Parser.parse[PureLogger](lines).value == expected)
  }

  test("parse invalid quote") {
    assert(
      Parser.parse[PureLogger]("#+begin_quote".linesIterator).value == ParserError(
        "End marker: '#+end_quote' not found"
      ).asLeft
    )
  }

}
