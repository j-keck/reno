package reno.pdf

import java.awt.geom.Rectangle2D

import org.scalacheck.Prop.{forAll, propBoolean}
import org.scalacheck.Properties
import org.scalacheck.Gen._
import reno.RenoScalaCheckInstances._
import org.scalactic.TripleEquals._

object Rect2DSpec extends Properties("Rect2D") {

  property("fromRectangle2D") = forAll(posNum[Float], posNum[Float], posNum[Float], posNum[Float]) { (x, y, w, h) =>
    val rect = Rect2D.fromRectangle2D(new Rectangle2D.Float(x, y, w, h))
    ("leftBottom.col" |: rect.leftBottom.col === x) &&
    ("leftBottom.row" |: rect.leftBottom.row === y) &&
    ("rightTop.col" |: rect.rightTop.col === x + w) &&
    ("rightTop.row" |: rect.rightTop.row === y + h)
  }

  property("toRectangle2D / fromRectangle2D") = forAll { rect: Rect2D =>
    val res = Rect2D.fromRectangle2D(rect.toRectangle2D)
    ("leftBottom" |: res.leftBottom === rect.leftBottom) &&
    ("rightTop" |: res.rightTop === rect.rightTop)
  }
}
