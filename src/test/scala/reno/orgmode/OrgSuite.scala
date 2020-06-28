package reno
package orgmode

import org.scalatest.funsuite.AnyFunSuite
import reno.pdf.{Mark, Pdf, Pos, Rect2D, TextMarkupAnnotation}
import cats.data.{Chain, NonEmptyList, Writer}
import cats.implicits._
import io.chrisdavenport.log4cats.extras.{LogMessage, WriterLogger}

class OrgSuite extends AnyFunSuite {
  type PureLogger[A] = Writer[Chain[LogMessage], A]
  implicit def pureLogger = WriterLogger[Chain]()

  test("update - unchanged") {
    val org = Org(
      List.empty,
      mkNotes(
        ("text01", Seq("id01")),
        ("text02", Seq("id02"))
      )
    )

    val (logs, updated) = org
      .update[PureLogger](
        mkPDFWithAnnotations(
          ("text01", "id01"),
          ("text02", "id02")
        )
      )
      .run

    assert(logs.count(_.message.contains("unchanged")) == 2L)
    assert(updated === org)
  }

  test("update - notes merged") {
    val org = Org(
      List.empty,
      mkNotes(
        (
          "text01\ntext02\ntext03",
          Seq("id01", "id02", "id03")
        )
      )
    )

    val updated = org
      .update[PureLogger](
        mkPDFWithAnnotations(
          ("text01", "id01"),
          ("text02", "id02"),
          ("text03", "id03"),
          ("text04", "id04")
        )
      )
      .value

    assert(updated.notes.map(_.text) === Seq("text01\ntext02\ntext03", "text04"))
  }

  test("update - new annotations") {
    val org = Org(
      List.empty,
      mkNotes(
        ("text01", Seq("id01")),
        ("free-text", Seq.empty),
        ("text02", Seq("id02"))
      )
    )

    val updated = org
      .update[PureLogger](
        mkPDFWithAnnotations(
          ("text01", "id01"),
          ("after free-text", "new-id01"),
          ("text02", "id02"),
          ("after text02", "new-id02")
        )
      )
      .value

    assert(
      updated.notes.map(_.text) === Seq("text01", "free-text", "after free-text", "text02", "after text02")
    )
  }

  def mkPDFWithAnnotations(x: (String, String)*): Pdf =
    Pdf(RenoScalaCheckInstances.genPdfInfo.sample.get, mkAnnotations(x: _*))

  def mkNotes(x: (String, Seq[String])*): List[Note] =
    x.map { case (text, ids) => mkNote(text, ids) }.toList

  def mkAnnotations(x: (String, String)*): List[TextMarkupAnnotation] =
    x.map { case (text, id) => mkAnnotation(text, id) }.toList

  def mkNote(text: String, ids: Seq[String]): Note =
    Note.quote(text, ids)

  def mkAnnotation(text: String, id: String): TextMarkupAnnotation =
    TextMarkupAnnotation(0, Mark(id, NonEmptyList.one(Rect2D(Pos(0, 0), Pos(0, 0)))), text)
}
