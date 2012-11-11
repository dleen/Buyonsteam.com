package scrapers

import scala.util.control.Exception.catching

import org.jsoup.HttpStatusException
import org.jsoup.Jsoup

import akka.actor._
import akka.util.Duration
import akka.util.duration.intToDurationInt

import models._

abstract class Scraper extends Actor

object Scraper {

  def rm$(s: String): Double = {
    if (s(0) == '$') s.tail.toDouble
    else scala.Double.PositiveInfinity
  }

  def $anitizer(price: String): Double = {
    // "$59.99" -> 59.99 
    // "$59.99 $49.99" -> 49.99 
    // "$59.99$49.99" -> 49.99 
    if (price.contains('$')) {
      if (price.contains('%')) {
        val ind = price.indexOf('%')
        rm$(price.drop(ind + 1))
      }
      price.split('$').flatMap(_.split(' ')).filterNot(_.isEmpty).map(_.toDouble).min
    } else {
      try {
        price.toDouble
      } catch {
        case e => 0
      }
    }
  }

  def checkSite(url: String, ping: Option[org.jsoup.Connection.Response]) = {
    def checker(count: Int, conStat: Option[org.jsoup.Connection.Response]): Option[org.jsoup.nodes.Document] = {
      if (conStat.map(_.statusCode).getOrElse(0) == 200) Some(conStat.get.parse)
      else if (count > 24) None
      else {
        //println(count)
        checker(count + 1,
          catching(classOf[java.net.SocketTimeoutException], classOf[org.jsoup.HttpStatusException],
            classOf[java.lang.ExceptionInInitializerError]) opt Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
            .timeout(3000).execute())
      }
    }
    checker(0, ping)
  }

  def matchExact = {
    def matchEx(matched: Int, acc: Int): Int = {
      if (matched == 0) acc
      else {
        val numMatched = DataCleanup.matchExactNames
        matchEx(numMatched, acc + numMatched)
      }
    }
    matchEx(1, 0)
  }

  def matchSimilar = {
    def matchEx(matched: Int, acc: Int): Int = {
      if (matched == 0) acc
      else {
        val numMatched = DataCleanup.matchSimilarNames
        matchEx(numMatched, acc + numMatched)
      }
    }
    matchEx(1, 0)
  }

}

sealed trait ScrapedMessage
case class FetchGame(pageN: Int) extends ScrapedMessage
case class GameFetchedG(gl: List[GwithP], pageN: Int, success: Boolean) extends ScrapedMessage
case class GameFetched(gl: List[GwithP]) extends ScrapedMessage
case class GameFetchedS(gl: List[GSwP], pageN: Int, success: Boolean) extends ScrapedMessage
case object Scrape extends ScrapedMessage
case object Gogo extends ScrapedMessage
case object Gogo1 extends ScrapedMessage
case class Finished(who: String, duration: Duration) extends ScrapedMessage

class Listener extends Actor {

  var nrOfResults: Int = 0
  var totalTime: Duration = 0.millis

  val start: Long = System.currentTimeMillis

  def incre(duration: Duration) = {
    nrOfResults += 1
    totalTime += duration
    println("RESULTS: " + nrOfResults.toString)
    if (nrOfResults == 5) {
      val matex = Scraper.matchExact
      val matsim = Scraper.matchSimilar

      println("Number exact matched: " + matex.toString)
      println("Number exact similar: " + matsim.toString)

      println("****** ALL DONE ******")
      println("TOTAL TIME TAKEN: " + totalTime.toString)
      println("****** ALL DONE ******")

      context.system.shutdown()
    }
  }

  def receive = {
    case Finished(who, duration) => {
      who match {
        case "DlGamer" => incre(duration)
        case "GamersGate" => incre(duration)
        case "GameStop" => incre(duration)
        case "GreenmanGaming" => incre(duration)
        case "Steam" => incre(duration)
      }
    }
  }

}

class Runner extends Actor {

  def receive = {
    case Gogo => Runner.scrapeEverything
    case Gogo1 => println("Arf arf")
  }

}

object Runner {

  def scrapeEverything = {

    val system = ActorSystem("ScraperSystem")

    val listener = system.actorOf(Props[Listener], name = "listener")

    val GMmaster = system.actorOf(Props(new GreenmanGamingMaster(listener)), name = "GMmaster")
    val GSmaster = system.actorOf(Props(new GameStopMaster(listener)), name = "GSmaster")
    val Dlmaster = system.actorOf(Props(new DlGamerMaster(listener)), name = "Dlmaster")
    val GGmaster = system.actorOf(Props(new GamersGateMaster(listener)), name = "GGmaster")
    val Stmaster = system.actorOf(Props(new SteamMaster(listener)), name = "Stmaster")

    GSmaster ! Scrape
    Dlmaster ! Scrape
    GGmaster ! Scrape
    Stmaster ! Scrape
    GMmaster ! Scrape

  }

}