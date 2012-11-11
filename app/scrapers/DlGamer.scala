package scrapers

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

class DlGamerScraper extends Scraper {

  def receive = {
    case FetchGame(pageN) => sender ! DlGamerScraper.getAll(pageN)
  }

}

object DlGamerScraper {

  private val name = "DlGamer"

  private val storeHead = "http://www.dlgamer.us/download-pc_games-c-27.html?page="

  // Fix this
  /*def getFinalPage(storeTail: String = "1"): Int = {
    val doc = Jsoup.connect(storeHead + storeTail).get()
    val paginator = doc.getElementsByClass("numberpage").select("a").toList
    val last = paginator.last.text
    if (last == storeTail) storeTail.toInt
    else getFinalPage(last)
  }*/

  // lol solution
  val finalPage = 40

  private def getAll(pageN: Int): GameFetchedG = {

    val url = DlGamerScraper.storeHead + pageN.toString
    val ping = catching(classOf[java.net.SocketTimeoutException], classOf[org.jsoup.HttpStatusException], classOf[java.lang.ExceptionInInitializerError]) opt Jsoup.connect(url)
      .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
      .timeout(3000).execute()

    def allVals(html: Element): GwithP = GwithP(gameVals(html), priceVals(html))

    def gameVals(html: Element): Game = {
      val name = html.getElementsByClass("title").text
      val gameUrl = html.getElementsByClass("title").select("a").attr("href")
      val imgUrl = "http://dlgamer.us/" + html.select("img").attr("src")

      Game(NotAssigned, name, DlGamerScraper.name, gameUrl, imgUrl)
    }

    def priceVals(html: Element): Price = {
      val name = html.getElementsByClass("title").text
      val priceS = Scraper.$anitizer(html.getElementsByClass("price").text)
      val onSale = !html.getElementsByClass("old-price").text.isEmpty

      Price(NotAssigned, priceS, onSale, new Date(), 0)
    }

    val doc = Scraper.checkSite(url, ping).getOrElse(org.jsoup.nodes.Document.createShell(""))

    if (!doc.body.hasText) GameFetchedG(List(), pageN, false)
    else {
      val searchResults = doc.getElementsByClass("box").toList
      GameFetchedG(searchResults map (allVals), pageN, true)

    }
  }

}

class DlGamerMaster(listener: ActorRef) extends Actor {

  private val Dlfetcher = context.actorOf(Props[DlGamerScraper].withRouter(RoundRobinRouter(4)), name = "Dlfetcher")

  var nrOfResults: Int = _
  val start: Long = System.currentTimeMillis

  println(DlGamerScraper.finalPage)

  def receive = {
    case Scrape => for (i <- 1 to DlGamerScraper.finalPage) Dlfetcher ! FetchGame(i)
    case GameFetchedG(gl, _, true) => {
      gl flatMap (x => catching(classOf[PSQLException]) opt GwithP.insertGame(x))
      gl flatMap (x => catching(classOf[PSQLException]) opt GwithP.insertPrice(x))

      //printf("Dl:%d ".format(nrOfResults))
      nrOfResults += 1

      if (nrOfResults == DlGamerScraper.finalPage) {
        println("Dl done in: %s".format((System.currentTimeMillis - start).millis))

        listener ! Finished("DlGamer", (System.currentTimeMillis - start).millis)
        context.stop(self)
      }
    }
    case GameFetchedG(List(), pageN, false) => {
      println("Problem on Dl page: " + pageN.toString)
      nrOfResults += 1

      if (nrOfResults == DlGamerScraper.finalPage) {
        println("Dl done in: %s ".format((System.currentTimeMillis - start).millis))

        listener ! Finished("DlGamer", (System.currentTimeMillis - start).millis)
        context.stop(self)
      }
    }
    case e => {
      println("Printing the error:")
      println(e)
    }
  }

}
