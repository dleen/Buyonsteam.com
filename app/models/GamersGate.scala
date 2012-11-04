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
import akka.routing.RoundRobinRouter

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
    val url = storeHead + "1"
    val ping = catching(classOf[java.net.SocketTimeoutException], classOf[org.jsoup.HttpStatusException]) opt Jsoup.connect(url)
      .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
      .timeout(3000).execute()
    val doc = Scraper.checkSite(url, ping).getOrElse(org.jsoup.nodes.Document.createShell(""))

    if (!doc.body.hasText) 1
    else {
      val navNumStr = doc.getElementsByClass("paginator").select("a").text
      val navNumSep = navNumStr.split(' ').toList

      navNumSep map { _.toInt } max
    }
  }

  private def getAll(pageN: Int): GameFetchedG = {

    val url = GamersGateScraper.storeHead + pageN.toString
    val ping = catching(classOf[java.net.SocketTimeoutException], classOf[org.jsoup.HttpStatusException]) opt Jsoup.connect(url)
      .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
      .timeout(3000).execute()

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

    val doc = Scraper.checkSite(url, ping).getOrElse(org.jsoup.nodes.Document.createShell(""))

    if (!doc.body.hasText) GameFetchedG(List(), pageN, false)
    else {
      val searchResults = doc.getElementsByClass("product_display").toList
      GameFetchedG(searchResults map (allVals), pageN, true)
    }
  }

}

class GamersGateMaster(listener: ActorRef) extends Actor {

  private val GGfetcher = context.actorOf(Props[GamersGateScraper].withRouter(RoundRobinRouter(4)), name = "GGfetcher")

  var nrOfResults: Int = _
  val start: Long = System.currentTimeMillis

  println(GamersGateScraper.finalPage)

  def receive = {
    case Scrape => for (i <- 1 to GamersGateScraper.finalPage) GGfetcher ! FetchGame(i)
    case GameFetchedG(gl, _, true) => {
      gl flatMap (x => catching(classOf[PSQLException]) opt GwithP.insertGame(x))
      gl flatMap (x => catching(classOf[PSQLException]) opt GwithP.insertPrice(x))

      printf("GG:%d ".format(nrOfResults))
      nrOfResults += 1

      if (nrOfResults == GamersGateScraper.finalPage) {
        println("All done in: %s".format((System.currentTimeMillis - start).millis))

        listener ! Finished("GamersGate", (System.currentTimeMillis - start).millis)
        context.stop(self)
      }
    }
    case GameFetchedG(List(), pageN, false) => {
      println("Problem on GG page: " + pageN.toString)
      nrOfResults += 1

      if (nrOfResults == GamersGateScraper.finalPage) {
        printf("GG done in: %s ".format((System.currentTimeMillis - start).millis))

        listener ! Finished("GamersGate", (System.currentTimeMillis - start).millis)
        context.stop(self)
      }
    }
    case e => {
      println("Printing the error:")
      println(e)
    }
  }

}