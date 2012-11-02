package models

import anorm._
import java.util.Date
import org.jsoup._
import org.jsoup.nodes._
import scala.collection.JavaConversions._

abstract class Scraper {

  def getAll: List[GwithP]
  def getGames: List[Game]
  def getPrices: List[Price]

  def scrapePage[A](pageN: Int, f: (Element) => A): List[A]

  // Capture everything from html, useful for initializing, creating new games.
  def allVals(html: Element): GwithP = GwithP(gameVals(html), priceVals(html))

  // Capture the game values
  def gameVals(html: Element): Game

  // Capture the price values
  def priceVals(html: Element): Price

}

trait SafeMoney {

  def rm$(s: String): Double = {
    if (s(0) == '$') s.tail.toDouble
    else scala.Double.PositiveInfinity
  }

  def $anitizer(price: String): Double = {
    // "$59.99" -> 59.99 
    if (price.contains('$')) {
      if (price.contains('%')) {
        val ind = price.indexOf('%')
        rm$(price.drop(ind + 1))
      }
      price.split('$').flatMap(_.split(' ')).filterNot(_.isEmpty).map(_.toDouble).min
    } else {
      try {
        price.toDouble
      } catch {
        case e => 0
      }
    }
  }

}