package scrapers

import java.lang.ExceptionInInitializerError
import java.net.SocketTimeoutException
import java.util.Date

import scala.Option.option2Iterable
import scala.collection.JavaConversions.asScalaBuffer
import scala.util.control.Exception.catching

import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.postgresql.util.PSQLException

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.routing.RoundRobinRouter
import akka.util.duration.longToDurationLong
import anorm.NotAssigned

import models._

class GreenmanGamingScraper extends Scraper {

  def receive = {
    case FetchGame(pageN) => sender ! GreenmanGamingScraper.getAll(pageN)
  }

}

object GreenmanGamingScraper {

  private val name = "GreenmanGaming"

  private val storeHead = "http://www.greenmangaming.com/s/us/en/pc/games/?page="

  val finalPage = {
    val url = storeHead + "1"
    val ping = catching(classOf[java.net.SocketTimeoutException], classOf[org.jsoup.HttpStatusException], classOf[java.lang.ExceptionInInitializerError]) opt Jsoup.connect(url)
      .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
      .timeout(3000).execute()
    val doc = Scraper.checkSite(url, ping).getOrElse(org.jsoup.nodes.Document.createShell(""))

    if (!doc.body.hasText) 1
    else {
      val navNumStr = doc.getElementsByClass("paginator").select("a").text
      val navNumSep = navNumStr.split(' ').toList

      navNumSep(2).toInt
    }

  }

  private def getAll(pageN: Int): GameFetchedG = {

    val url = GreenmanGamingScraper.storeHead + pageN.toString
    val ping = catching(classOf[java.net.SocketTimeoutException], classOf[org.jsoup.HttpStatusException], classOf[java.lang.ExceptionInInitializerError]) opt Jsoup.connect(url)
      .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
      .timeout(3000).execute()

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

    val doc = Scraper.checkSite(url, ping).getOrElse(org.jsoup.nodes.Document.createShell(""))

    if (!doc.body.hasText) GameFetchedG(List(), pageN, false)
    else {
      val searchResults = doc.select("li.border-container").toList
      GameFetchedG(searchResults map (allVals), pageN, true)

    }
  }

}

class GreenmanGamingMaster(listener: ActorRef) extends Actor {

  private val GMfetcher = context.actorOf(Props[GreenmanGamingScraper].withRouter(RoundRobinRouter(4)), name = "GMfetcher")

  var nrOfResults: Int = _
  val start: Long = System.currentTimeMillis

  println(GreenmanGamingScraper.finalPage)

  def receive = {
    case Scrape => for (i <- 1 to GreenmanGamingScraper.finalPage) GMfetcher ! FetchGame(i)
    case GameFetchedG(gl, _, true) => {
      gl flatMap (x => catching(classOf[PSQLException]) opt GwithP.insertGame(x))
      gl flatMap (x => catching(classOf[PSQLException]) opt GwithP.insertPrice(x))

      //printf("GM:%d ".format(nrOfResults))
      nrOfResults += 1

      if (nrOfResults == GreenmanGamingScraper.finalPage) {
        println("GM done in: %s ".format((System.currentTimeMillis - start).millis))

        listener ! Finished("GreenmanGaming", (System.currentTimeMillis - start).millis)
        context.stop(self)
      }
    }
    case GameFetchedG(List(), pageN, false) => {
      println("Problem on GM page: " + pageN.toString)
      nrOfResults += 1

      if (nrOfResults == GreenmanGamingScraper.finalPage) {
        println("GM done in: %s ".format((System.currentTimeMillis - start).millis))

        listener ! Finished("GreenmanGaming", (System.currentTimeMillis - start).millis)
        context.stop(self)
      }
    }
    case e => {
      println("Printing the error:")
      println(e)
    }
  }

}
