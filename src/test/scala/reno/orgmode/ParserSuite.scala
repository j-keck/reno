package reno.orgmode

import cats.data.{Chain, Writer}
import cats.syntax.either._
import io.chrisdavenport.log4cats.extras.{LogMessage, WriterLogger}
import org.scalatest.funsuite.AnyFunSuite

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
      |
      |\begin{align}
      |a &= b
      |\end{align}
      |:reno_marker_ids:
      |id-005
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
        Note.src("source with ids", Seq("id-003", "id-004")),
        Note.latexFragment("align", "a &= b", Seq("id-005"))
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

  test("takeBlock") {
    val Right((block, rest)) =
      Parser.takeBlock(List("start", "text1", "text2", "end", "text3"), "start", "end")
    assert(block === List("text1", "text2"))
    assert(rest === List("text3"))

    assert(Parser.takeBlock(Nil, "start", "end").isLeft)
  }

  test("takeIds") {
    val Right((ids, rest)) = Parser.takeIds(List("  ", "  ", ":reno_marker_ids:", "id01", "id02", ":end:", "text"))
    assert(ids === List("id01", "id02"))
    assert(rest === List("text"))
  }

}
