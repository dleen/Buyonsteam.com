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

  private def getAll(pageN: Int): GameFetchedG = {

    val url = GreenmanGamingScraper.storeHead + pageN.toString
    val ping = catching(classOf[java.net.SocketTimeoutException]) opt Jsoup.connect(url)
      .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
      .timeout(3000).execute()

    def checkSite(url: String) = {
      def checker(count: Int, conStat: Option[org.jsoup.Connection.Response]): Option[org.jsoup.nodes.Document] = {
        if (conStat.map(_.statusCode).getOrElse(0) == 200) Some(conStat.get.parse)
        else if (count > 1) None
        else {
          println(count)
          checker(count + 1,
            catching(classOf[java.net.SocketTimeoutException]) opt Jsoup.connect(url)
              .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
              .timeout(3000).execute())
        }
      }
      checker(0, ping)
    }

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

    val doc = checkSite(url).getOrElse(org.jsoup.nodes.Document.createShell(""))

    if (!doc.body.hasText) GameFetchedG(List(), pageN, false)
    else {
      val searchResults = doc.select("li.border-container").toList
      GameFetchedG(searchResults map (allVals), pageN, true)

    }
  }

}

class GreenmanGamingMaster extends Actor {

  private val fetcher = context.actorOf(Props[GreenmanGamingScraper])

  var nrOfResults: Int = _
  val start: Long = System.currentTimeMillis

  println(GreenmanGamingScraper.finalPage)

  def receive = {
    case Scrape => for (i <- 1 to GreenmanGamingScraper.finalPage) fetcher ! FetchGame(i)
    case GameFetchedG(gl, _, true) => {
      gl flatMap (x => catching(classOf[PSQLException]) opt GwithP.insertGame(x))
      gl flatMap (x => catching(classOf[PSQLException]) opt GwithP.insertPrice(x))

      printf("GM:%d ".format(nrOfResults))
      nrOfResults += 1

      if (nrOfResults == GreenmanGamingScraper.finalPage) {
        printf("GM done in: %s ".format((System.currentTimeMillis - start).millis))
        context.stop(self)
      }
    }
    case GameFetchedG(List(), pageN, false) => println("Problem on page: " + pageN.toString)
    case e => {
      println("Printing the error:")
      println(e)
    }
  }

}
