package reno.pdf

package object engine {}

package engine {

  import reno.pdf.engine.PageInfo.Orientation

  case class PageInfo(orientation: Orientation, width: Float, height: Float)

  object PageInfo {
    sealed trait Orientation
    object Orientation {
      case object Portrait  extends Orientation
      case object Landscape extends Orientation
    }

  }
}
