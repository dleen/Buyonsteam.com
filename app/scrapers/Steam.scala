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

class SteamScraper extends Scraper {

  def receive = {
    case FetchGame(pageN) => sender ! SteamScraper.getAllSteam(pageN)
  }

}

object SteamScraper {

  private def appId(url: String): Int = {
    url.substring(34, url.indexOf('/', 34)).toInt
  }

  private def scorefS(meta: String): Int = {
    if (meta.isEmpty) 0
    else meta.toInt
  }

  private val name = "Steam"

  private val storeHead =
    "http://store.steampowered.com/search/results?&cc=us&category1=998&page="

  val finalPage = {
    val url = storeHead + "1"
    val ping = catching(classOf[java.net.SocketTimeoutException], classOf[org.jsoup.HttpStatusException], classOf[java.lang.ExceptionInInitializerError]) opt Jsoup.connect(url)
      .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
      .timeout(3000).execute()
    val doc = Scraper.checkSite(url, ping).getOrElse(org.jsoup.nodes.Document.createShell(""))

    if (!doc.body.hasText) 1
    else {
      val navNumStr = doc.getElementsByClass("search_pagination_right").select("a").text
      val navNumSep = navNumStr.split(' ')

      navNumSep(2).toInt
    }
  }

  private def getAllSteam(pageN: Int): GameFetchedS = {

    val url = SteamScraper.storeHead + pageN.toString
    val ping = catching(classOf[java.net.SocketTimeoutException], classOf[org.jsoup.HttpStatusException], classOf[java.lang.ExceptionInInitializerError]) opt Jsoup.connect(url)
      .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
      .timeout(3000).execute()

    def steamAllVals(html: Element): GSwP = GSwP(steamVals(html), allVals(html))
    def allVals(html: Element): GwithP = GwithP(gameVals(html), priceVals(html))

    def gameVals(html: Element): Game = {
      val name = html.select("h4").text
      val gameUrl = html.select("a").attr("href")
      val imgUrl = html.getElementsByClass("search_capsule").select("img").attr("src")

      Game(NotAssigned, name, SteamScraper.name, gameUrl, imgUrl)
    }

    def priceVals(html: Element): Price = {
      val name = html.select("h4").text
      val priceS = Scraper.$anitizer(html.getElementsByClass("search_price").text)
      val onSale = !html.select("strike").isEmpty

      Price(NotAssigned, priceS, onSale, new Date(), 0)
    }

    def steamVals(html: Element): SteamGame = {
      val name = html.select("h4").text
      val releaseDate = html.getElementsByClass("search_released").text
      val meta = scorefS(html.getElementsByClass("search_metascore").text)
      val gameUrl = html.select("a").attr("href")
      val gameId = appId(gameUrl)

      SteamGame(gameId, name, releaseDate, meta, 0)
    }

    val doc = Scraper.checkSite(url, ping).getOrElse(org.jsoup.nodes.Document.createShell(""))

    if (!doc.body.hasText) GameFetchedS(List(), pageN, false)
    else {
      val searchResults = doc.getElementsByClass("search_result_row").toList
      GameFetchedS(searchResults map (steamAllVals), pageN, true)
    }
  }

}

class SteamMaster(listener: ActorRef) extends Actor {

  private val Stfetcher = context.actorOf(Props[SteamScraper].withRouter(RoundRobinRouter(4)), name = "Stfetcher")

  var nrOfResults: Int = _
  val start: Long = System.currentTimeMillis

  println(SteamScraper.finalPage)

  def receive = {
    case Scrape => for (i <- 1 to SteamScraper.finalPage) Stfetcher ! FetchGame(i)
    case GameFetchedS(gl, _, true) => {
      gl flatMap (x => catching(classOf[PSQLException]) opt GSwP.insertGame(x))
      gl flatMap (x => catching(classOf[PSQLException]) opt GSwP.insertPrice(x))
      gl flatMap (x => catching(classOf[PSQLException]) opt GSwP.insertSteam(x))

      printf("St:%d ".format(nrOfResults))
      nrOfResults += 1

      if (nrOfResults == SteamScraper.finalPage) {
        println("St done in: %s ".format((System.currentTimeMillis - start).millis))

        listener ! Finished("Steam", (System.currentTimeMillis - start).millis)
        context.stop(self)
      }
    }
    case GameFetchedS(List(), pageN, false) => {
      println("Problem on St page: " + pageN.toString)
      nrOfResults += 1

      if (nrOfResults == SteamScraper.finalPage) {
        printf("St done in: %s ".format((System.currentTimeMillis - start).millis))

        listener ! Finished("Steam", (System.currentTimeMillis - start).millis)
        context.stop(self)
      }
    }
    case e => {
      println("Printing the error:")
      println(e)
    }
  }

}