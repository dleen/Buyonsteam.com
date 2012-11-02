package controllers

import play.api._
import play.api.mvc._

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import views._
import models._

import scala.util.control.Exception._

import play.api.libs.json.Json._

import play.api.libs.concurrent._
import play.api.Play.current

import akka.util.Duration
import akka.util.duration._

import org.postgresql.util._

import org.jsoup._
import org.jsoup.nodes._
import scala.collection.JavaConversions._

object Application extends Controller {

  /*
   * Testing code.
   */

  def manualMatching = Action {
    Ok(html.manmatch(DataCleanup.matchManually))
  }

  def matchExact = Action {
    Ok(toJson(DataCleanup.matchExactNames))
  }

  def matchSimilar = Action {
    Ok(toJson(DataCleanup.matchSimilarNames))
  }

  def matchem(id1: Int, id2: Int) = Action {
    DataCleanup.equateIds(id1, id2)
    Ok(html.manmatch(DataCleanup.matchManually))
  }

  def blacklist(id1: Int, id2: Int, list: List[Int]) = id1 :: id2 :: list

  /*
   * Real working code.
   */

  def grabprices = Action {
    val promOfIndex: Promise[String] = Akka.future {
      val start: Long = System.currentTimeMillis

      for (i <- (1 to SteamScraper.finalPage).par) {
        SteamScraper(i).getAll map { x =>
          try { GwithP.insertPrice(x) }
          catch {
            case e => {
              println(e)
              println(x)
            }
          }
        }
      }

      for (j <- (1 to GamersGateScraper.finalPage).par) {
        //GamersGateScraper(i).getGames flatMap (x => catching(classOf[PSQLException]) opt Game.insertOtherStore(x))
        GamersGateScraper(j).getAll map { x =>
          try { GwithP.insertPrice(x) }
          catch {
            case e => {
              println(e)
              println(x)
            }
          }
        }
      }

      "Done! " + (System.currentTimeMillis - start).millis.toString
    }
    Async {
      promOfIndex.orTimeout("Oops", 180000).map { eitherIndorTimeout =>
        eitherIndorTimeout.fold(
          timeout => InternalServerError(timeout),
          i => Ok("All " + i))
      }
    }
  }

  def reindexGameStop = Action {
    val promOfIndex: Promise[String] = Akka.future {
      val start: Long = System.currentTimeMillis
      GameStopScraper.reindex
      "GameStop finished updating the database in: " + (System.currentTimeMillis - start).millis.toString
    }
    Async {
      promOfIndex.orTimeout("Oops", 300000).map { eitherIndorTimeout =>
        eitherIndorTimeout.fold(
          timeout => InternalServerError(timeout),
          i => Ok("All " + i))
      }
    }
  }

  def reindexDlGamer = Action {
    val promOfIndex: Promise[String] = Akka.future {
      val start: Long = System.currentTimeMillis
      DlGamerScraper.reindex
      "DlGamer finished updating the database in: " + (System.currentTimeMillis - start).millis.toString
    }
    Async {
      promOfIndex.orTimeout("Oops", 300000).map { eitherIndorTimeout =>
        eitherIndorTimeout.fold(
          timeout => InternalServerError(timeout),
          i => Ok("All " + i))
      }
    }
  }

  def reindexGamersGate = Action {
    val promOfIndex: Promise[String] = Akka.future {
      val start: Long = System.currentTimeMillis
      GamersGateScraper.reindex
      "GamersGate finished updating the database in: " + (System.currentTimeMillis - start).millis.toString
    }
    Async {
      promOfIndex.orTimeout("Oops", 300000).map { eitherIndorTimeout =>
        eitherIndorTimeout.fold(
          timeout => InternalServerError(timeout),
          i => Ok("All " + i))
      }
    }
  }

  def reindexGreenmanGaming = Action {
    val promOfIndex: Promise[String] = Akka.future {
      val start: Long = System.currentTimeMillis
      GreenmanGamingScraper.reindex
      "GreenmanGaming finished updating the database in: " + (System.currentTimeMillis - start).millis.toString
    }
    Async {
      promOfIndex.orTimeout("Oops", 300000).map { eitherIndorTimeout =>
        eitherIndorTimeout.fold(
          timeout => InternalServerError(timeout),
          i => Ok("All " + i))
      }
    }
  }

  def reindexSteam = Action {
    val promOfIndex: Promise[String] = Akka.future {
      val start: Long = System.currentTimeMillis
      SteamScraper.reindex
      "Steam finished updating the database in: " + (System.currentTimeMillis - start).millis.toString
    }
    Async {
      promOfIndex.orTimeout("Oops", 60000).map { eitherIndorTimeout =>
        eitherIndorTimeout.fold(
          timeout => InternalServerError(timeout),
          i => Ok("All " + i))
      }
    }
  }

  def autocompleteSearch(term: String) = Action { Ok(toJson(Game.findPartialName(term))) }

  def qlist(name: String) = Action {
    list(0, 2, name)
  }

  def list(page: Int, orderBy: Int, filter: String) = Action { implicit request =>
    Ok(html.list(Game.list(page = page, orderBy = orderBy, filter = (filter)), orderBy, filter))
  }

  val Home = Redirect(routes.Application.list(0, 2, ""))
  def index = Action { Home }

}
