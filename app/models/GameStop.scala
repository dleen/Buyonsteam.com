package models

import anorm.NotAssigned
import java.util.Date

import org.jsoup._
import org.jsoup.nodes._
import scala.collection.JavaConversions._

import scala.util.control.Exception._
import org.postgresql.util._

import akka.actor._
import akka.util.duration._
import akka.util.Duration

class GameStopScraper extends Scraper {

  def receive = {
    case FetchGame(pageN) => sender ! GameStopScraper.getAll(pageN)
  }

}

object GameStopScraper {

  private val name = "GameStop"

  private val storeHead = "http://www.gamestop.com/browse/pc?nav=2b"
  private val storeTail = ",138c-ffff2418"

  val finalPage = {
    val doc = Jsoup.connect(storeHead + storeTail).get()
    val navNumStr = doc.getElementsByClass("pagination_controls").select("strong").text
    val navNumSep = navNumStr.split(' ').toList.last.drop(1)

    navNumSep.toInt
  }

  private def getAll(pageN: Int): GameFetched = {

    val doc = Jsoup.connect(GameStopScraper.storeHead + ((pageN - 1) * 12).toString + GameStopScraper.storeTail)
      .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
      .timeout(30000)
      .get()

    val searchResults = doc.getElementsByClass("product").toList

    def allVals(html: Element): GwithP = GwithP(gameVals(html), priceVals(html))

    def gameVals(html: Element): Game = {
      val name = html.getElementsByClass("product_info").select("h3").select("a").text
      val gameUrl = "http://www.gamestop.com" +
        html.getElementsByClass("product_info").select("h3").select("a").attr("href")
      val imgUrl = "http://www.gamestop.com" +
        html.getElementsByClass("grid_2").select("img").attr("src")

      Game(NotAssigned, name, GameStopScraper.name, gameUrl, imgUrl)
    }

    def priceVals(html: Element): Price = {
      val name = html.getElementsByClass("product_info").select("a").text
      val priceS = Scraper.$anitizer(html.getElementsByClass("pricing").text)
      val onSale = !html.getElementsByClass("old_price").text.isEmpty

      Price(NotAssigned, priceS, onSale, new Date(), 0)
    }

    GameFetched(searchResults map (allVals))
  }

}

class GameStopMaster extends Actor {

  private val fetcher = context.actorOf(Props[GameStopScraper])

  var nrOfResults: Int = _
  val start: Long = System.currentTimeMillis

  println(GameStopScraper.finalPage)

  def receive = {
    case Scrape => for (i <- 1 to GameStopScraper.finalPage) fetcher ! FetchGame(i)
    case GameFetched(gl) => {
      gl flatMap (x => catching(classOf[PSQLException]) opt GwithP.insertGame(x))
      gl flatMap (x => catching(classOf[PSQLException]) opt GwithP.insertPrice(x))

      printf("GS:%d ".format(nrOfResults))
      nrOfResults += 1

      if (nrOfResults == GameStopScraper.finalPage) {
        println("All done in: %s".format((System.currentTimeMillis - start).millis))
        context.stop(self)
      }
    }
    case e => println(e)
  }

}
