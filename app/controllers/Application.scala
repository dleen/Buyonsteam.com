package controllers

import views._
import models._

import play.api._
import play.api.mvc._

import play.api.libs.json.Json._

import akka.util.Duration
import akka.util.duration._
import akka.actor._

object Application extends Controller {

  /*
   * Testing code.
   */
  val system = ActorSystem("ScraperSystem")
  val GSmaster = system.actorOf(Props(new GameStopMaster), name = "GSmaster")
  //val Smaster = system.actorOf(Props(new SteamMaster), name = "Smaster")
  //val Dlmaster = system.actorOf(Props(new DlGamerMaster), name = "Dlmaster")
  //val GGmaster = system.actorOf(Props(new GamersGateMaster), name = "GGmaster")
  //val GMmaster = system.actorOf(Props(new GreenmanGamingMaster), name = "GMmaster")

  GSmaster ! Scrape

  //Smaster ! Scrape

  //Dlmaster ! Scrape

  //GGmaster ! Scrape

  //GMmaster ! Scrape

  system.shutdown()

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

  /*
   * Real working code.
   */

  /*  def reindexer(store: String) = {
    Action {
      val promOfIndex: Promise[String] = Akka.future {
        val start: Long = System.currentTimeMillis
        store match {
          case "steam" => SteamScraper.reindex
          case "greenman" => GreenmanGamingScraper.reindex
          case "gamestop" => GameStopScraper.reindex
          case "dlgamer" => DlGamerScraper.reindex
          case "gamersgate" => GamersGateScraper.reindex
        }
        store + " finished updating the database in: " + (System.currentTimeMillis - start).millis.toString
      }
      Async {
        promOfIndex.orTimeout("Oops", 300000).map { eitherIndorTimeout =>
          eitherIndorTimeout.fold(
            timeout => InternalServerError(timeout),
            i => Ok("All " + i))
        }
      }
    }
  }*/

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
