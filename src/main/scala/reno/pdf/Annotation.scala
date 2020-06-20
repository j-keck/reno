package reno.pdf

sealed trait Annotation {
  val pageNumber: Int
  val mark: Mark
}

object Annotation {
  implicit val orderingAnnotation: Ordering[Annotation] =
    (x: Annotation, y: Annotation) =>
      x.pageNumber compare y.pageNumber match {
        case 0 => Ordering[Mark].compare(x.mark, y.mark)
        case x => x
      }
}

case class TextMarkupAnnotation(pageNumber: Int, mark: Mark, text: String) extends Annotation
object TextMarkupAnnotation {
  implicit val orderingTextMarkupAnnotation: Ordering[TextMarkupAnnotation] =
    (x: TextMarkupAnnotation, y: TextMarkupAnnotation) => Annotation.orderingAnnotation.compare(x, y)
}
