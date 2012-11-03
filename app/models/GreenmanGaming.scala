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

class GreenmanGamingScraper extends Scraper {

  def receive = {
    case FetchGame(pageN) => sender ! GreenmanGamingScraper.getAll(pageN)
  }

}

object GreenmanGamingScraper {

  private val name = "GreenmanGaming"

  private val storeHead = "http://www.greenmangaming.com/s/us/en/pc/games/?page="

  val finalPage = {
    val doc = Jsoup.connect(storeHead + "1").get()
    val navNumStr = doc.getElementsByClass("paginator").select("a").text
    val navNumSep = navNumStr.split(' ').toList

    navNumSep(2).toInt
  }

  private def getAll(pageN: Int): GameFetched = {

    val doc = Jsoup.connect(GreenmanGamingScraper.storeHead + pageN.toString)
      .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
      .timeout(30000)
      .get()
    val searchResults = doc.select("li.border-container").toList

    def allVals(html: Element): GwithP = GwithP(gameVals(html), priceVals(html))

    def gameVals(html: Element): Game = {
      val name = html.select("h2").text
      val gameUrl = "http://www.greenmangaming.com" + html.select("a").attr("href")
      val imgUrl = html.select("img.cover").attr("src")

      Game(NotAssigned, name, GreenmanGamingScraper.name, gameUrl, imgUrl)
    }

    def priceVals(html: Element): Price = {
      val name = html.select("h2").text
      val priceS = Scraper.$anitizer(html.select("strong.curPrice").text)
      val onSale = !html.select("span.lt").isEmpty

      Price(NotAssigned, priceS, onSale, new Date(), 0)
    }

    GameFetched(searchResults map (allVals))
  }

}

class GreenmanGamingMaster extends Actor {

  private val fetcher = context.actorOf(Props[GreenmanGamingScraper])

  var nrOfResults: Int = _
  val start: Long = System.currentTimeMillis

  println(GreenmanGamingScraper.finalPage)

  def receive = {
    case Scrape => for (i <- 1 to GreenmanGamingScraper.finalPage) fetcher ! FetchGame(i)
    case GameFetched(gl) => {
      gl flatMap (x => catching(classOf[PSQLException]) opt GwithP.insertGame(x))
      gl flatMap (x => catching(classOf[PSQLException]) opt GwithP.insertPrice(x))

      printf("GM:%d ".format(nrOfResults))
      nrOfResults += 1

      if (nrOfResults == GreenmanGamingScraper.finalPage) {
        printf("GM done in: %s ".format((System.currentTimeMillis - start).millis))
        context.stop(self)
      }
    }
    case e => {
      println("Printing the error:")
      println(e)
    }
  }

}
