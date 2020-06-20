package reno

import scala.collection.LinearSeq

package object orgmode {

  // org header
  type Header = List[(String, String)]

  // notes / text
  type Notes = LinearSeq[Note]

}

package orgmode {

  case class Note(tpe: NoteType, text: String, ids: Seq[String])

  object Note {
    import NoteType._
    def heading(text: String, ids: Seq[String] = Seq.empty): Note = Note(Heading, text, ids)
    def quote(text: String, ids: Seq[String] = Seq.empty): Note   = Note(Quote, text, ids)
    def src(text: String, ids: Seq[String] = Seq.empty): Note     = Note(Src, text, ids)
    def text(text: String, ids: Seq[String] = Seq.empty): Note    = Note(Text, text, ids)
  }

  sealed trait NoteType
  object NoteType {
    case object Heading extends NoteType
    case object Quote   extends NoteType
    case object Src     extends NoteType
    case object Text    extends NoteType
  }
}
