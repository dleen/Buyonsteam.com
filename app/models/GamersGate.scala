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

class GamersGateScraper extends Scraper {

  def receive = {
    case FetchGame(pageN) => sender ! GamersGateScraper.getAll(pageN)
  }

}

object GamersGateScraper {

  private val name = "GamersGate"

  private val storeHead =
    "http://www.gamersgate.com/games?filter=available&pg="

  val finalPage = {
    val doc = Jsoup.connect(storeHead + "1").get()
    val navNumStr = doc.getElementsByClass("paginator").select("a").text
    val navNumSep = navNumStr.split(' ').toList

    navNumSep map { _.toInt } max
  }

  private def getAll(pageN: Int): GameFetched = {

    val doc = Jsoup.connect(GamersGateScraper.storeHead + pageN.toString)
      .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
      .get()
    val searchResults = doc.getElementsByClass("product_display").toList

    def allVals(html: Element): GwithP = GwithP(gameVals(html), priceVals(html))

    def gameVals(html: Element): Game = {
      val name = html.getElementsByClass("ttl").attr("title")
      val gameUrl = html.getElementsByClass("ttl").attr("href")
      val imgUrl = html.getElementsByClass("box_cont").select("img").attr("src")

      Game(NotAssigned, name, GamersGateScraper.name, gameUrl, imgUrl)
    }

    def priceVals(html: Element): Price = {
      val name = html.getElementsByClass("ttl").attr("title")
      val priceS = if (html.hasClass("prtag")) {
        Scraper.$anitizer(html.getElementsByClass("prtag").select("span")(1).ownText)
      } else 0

      val onSale = !html.getElementsByClass("discount").isEmpty

      Price(NotAssigned, priceS, onSale, new Date(), 0)
    }

    GameFetched(searchResults map (allVals))
  }

}

class GamersGateMaster extends Actor {

  private val fetcher = context.actorOf(Props[GamersGateScraper])

  var nrOfResults: Int = _
  val start: Long = System.currentTimeMillis

  println(GamersGateScraper.finalPage)

  def receive = {
    case Scrape => for (i <- 1 to GamersGateScraper.finalPage) fetcher ! FetchGame(i)
    case GameFetched(gl) => {
      gl flatMap (x => catching(classOf[PSQLException]) opt GwithP.insertGame(x))
      gl flatMap (x => catching(classOf[PSQLException]) opt GwithP.insertPrice(x))

      printf("GG:%d ".format(nrOfResults))
      nrOfResults += 1

      if (nrOfResults == GamersGateScraper.finalPage) {
        println("All done in: %s".format((System.currentTimeMillis - start).millis))
        context.stop(self)
      }
    }
    case e => println(e)
  }

}