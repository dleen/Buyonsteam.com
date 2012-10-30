package models

import anorm._
import java.util.Date
import org.jsoup._
import org.jsoup.nodes._
import scala.collection.JavaConversions._

abstract class Scraper {

  def getAll: List[Combined]
  def getGames: List[Game]
  def getPrices: List[Price]

  def scrapePage[A](pageN: Int, f: (Element) => A): List[A]

  // Capture everything from html, useful for initializing, creating new games.
  def allVals(html: Element): Combined = Combined(gameVals(html), priceVals(html))

  // Capture the game values
  def gameVals(html: Element): Game

  // Capture the price values
  def priceVals(html: Element): Price

}

abstract class StoreDetails {

  val storeHead: String
  val finalPage: Int

}

trait SafeMoney {

  def rm$(s: String): Double = if (s(0) == '$') s.tail.toDouble else scala.Double.PositiveInfinity
  def $anitizer(price: String): Option[Double] = {
    // "$59.99" -> 59.99 
    if (price.contains('$')) {
      if (price.contains(' ')) {
        val discount = price.split(' ')
        // "$59.99 $49.99" -> 49.99
        Some(math.min(rm$(discount(0)), rm$(discount(1))))
      } else Some(rm$(price))
    } else {
      try {
        Some(price.toDouble)
      } catch {
        case e => None
      }
    }
  }
}