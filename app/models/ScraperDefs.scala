package models

import akka.actor.Actor

abstract class Scraper extends Actor

object Scraper {

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

sealed trait ScrapedMessage
case class FetchGame(pageN: Int) extends ScrapedMessage
case class GameFetched(gl: List[GwithP]) extends ScrapedMessage
case class GameFetchedS(gl: List[GSwP], pageN: Int) extends ScrapedMessage
case object Scrape extends ScrapedMessage