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

class DlGamerScraper extends Scraper {

  def receive = {
    case FetchGame(pageN) => sender ! DlGamerScraper.getAll(pageN)
  }

}

object DlGamerScraper {

  private val name = "DlGamer"

  private val storeHead =
    "http://www.dlgamer.us/download-pc_games-c-27.html?page="

  // Fix this
  def getFinalPage(storeTail: String = "1"): Int = {
    val doc = Jsoup.connect(storeHead + storeTail).get()
    val paginator = doc.getElementsByClass("numberpage").select("a").toList
    val last = paginator.last.text
    if (last == storeTail) storeTail.toInt
    else getFinalPage(last)
  }

  // lol solution
  val finalPage = 40

  private def getAll(pageN: Int): GameFetched = {

    val doc = Jsoup.connect(DlGamerScraper.storeHead + pageN.toString)
      .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
      .timeout(30000)
      .get()

    val searchResults = doc.getElementsByClass("box").toList

    def allVals(html: Element): GwithP = GwithP(gameVals(html), priceVals(html))

    def gameVals(html: Element): Game = {
      val name = html.getElementsByClass("title").text
      val gameUrl = html.getElementsByClass("title").select("a").attr("href")
      val imgUrl = "http://dlgamer.us" + html.select("img").attr("src")

      Game(NotAssigned, name, DlGamerScraper.name, gameUrl, imgUrl)
    }

    def priceVals(html: Element): Price = {
      val name = html.getElementsByClass("title").text
      val priceS = Scraper.$anitizer(html.getElementsByClass("price").text)
      val onSale = !html.getElementsByClass("old-price").text.isEmpty

      Price(NotAssigned, priceS, onSale, new Date(), 0)
    }

    GameFetched(searchResults map (allVals))
  }

}

class DlGamerMaster extends Actor {

  private val fetcher = context.actorOf(Props[DlGamerScraper])

  var nrOfResults: Int = _
  val start: Long = System.currentTimeMillis

  println(DlGamerScraper.finalPage)

  def receive = {
    case Scrape => for (i <- 1 to DlGamerScraper.finalPage) fetcher ! FetchGame(i)
    case GameFetched(gl) => {
      gl flatMap (x => catching(classOf[PSQLException]) opt GwithP.insertGame(x))
      gl flatMap (x => catching(classOf[PSQLException]) opt GwithP.insertPrice(x))

      printf("Dl:%d ".format(nrOfResults))
      nrOfResults += 1

      if (nrOfResults == DlGamerScraper.finalPage) {
        println("All done in: %s".format((System.currentTimeMillis - start).millis))
        context.stop(self)
      }
    }
    case e => println(e)
  }

}
