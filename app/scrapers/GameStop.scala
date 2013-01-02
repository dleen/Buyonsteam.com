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

import scala.util.control.Exception._

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.routing.SmallestMailboxRouter
import akka.util.duration.longToDurationLong
import anorm.NotAssigned
import models.Game
import models.GwithP
import models.Price

class GameStopScraper extends Scraper {

  def receive = {
    case FetchGame(pageN) => sender ! GameStopScraper.getAll(pageN)
  }

}

object GameStopScraper {

  private val name = "GameStop"

  private val storeHead = "http://www.gamestop.com/browse/pc?nav=2b"
  private val storeTail = ",138c-ffff2418-51"

  val finalPage = {
    val url = storeHead + storeTail

    val ping = allCatch opt Jsoup.connect(url)
      .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
      .cookie("user_country", "US").timeout(3000).execute()
    val doc = Scraper.checkSite(url, ping).getOrElse(org.jsoup.nodes.Document.createShell(""))

    if (!doc.body.hasText) 1
    else {
      val navNumStr = doc.getElementsByClass("pagination_controls").select("strong").text
      val navNumSep = navNumStr.split(' ').toList.last.drop(1)

      navNumSep.toInt
    }

  }

  private def getAll(pageN: Int): GameFetchedG = {

    val url = GameStopScraper.storeHead + ((pageN - 1) * 12).toString + GameStopScraper.storeTail
    val ping = allCatch opt Jsoup.connect(url)
      .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
      .cookie("user_country", "US").timeout(3000).execute()

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

    val doc = Scraper.checkSite(url, ping).getOrElse(org.jsoup.nodes.Document.createShell(""))

    if (!doc.body.hasText) GameFetchedG(List(), pageN, false)
    else {
      val searchResults = doc.getElementsByClass("product").toList
      GameFetchedG(searchResults map (allVals), pageN, true)
    }
  }
}

class GameStopMaster(listener: ActorRef) extends Actor {

  private val GSfetcher = context.actorOf(Props[GameStopScraper].withRouter(SmallestMailboxRouter(16)), name = "GSfetcher")

  var nrOfResults: Int = _
  val start: Long = System.currentTimeMillis

  val finalPage = GameStopScraper.finalPage

  println(finalPage)

  def receive = {
    case Scrape => for (i <- 1 to finalPage) GSfetcher ! FetchGame(i)
    case GameFetchedG(gl, _, true) => {
      gl flatMap (x => catching(classOf[PSQLException]) opt GwithP.insertGame(x))
      gl flatMap (x => catching(classOf[PSQLException]) opt GwithP.insertPrice(x))

      printf("GS:%d ".format(nrOfResults))
      nrOfResults += 1

      if (nrOfResults == finalPage) {
        println("GS done in: %s".format((System.currentTimeMillis - start).millis))

        listener ! Finished("GameStop", (System.currentTimeMillis - start).millis)
        context.stop(self)
      }
    }
    case GameFetchedG(List(), pageN, false) => {
      println("Problem on GS page: " + pageN.toString)
      nrOfResults += 1

      if (nrOfResults == finalPage) {
        println("GS done in: %s ".format((System.currentTimeMillis - start).millis))

        listener ! Finished("GameStop", (System.currentTimeMillis - start).millis)
        context.stop(self)
      }
    }
    case e => {
      println("Printing the error:")
      println(e)
    }
  }

}
