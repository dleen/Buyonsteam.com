package controllers

import anorm.NotAssigned

import views._
import models._

import play.api._
import play.api.mvc._

import play.api.libs.json.Json._

import akka.actor._

import org.jsoup._
import org.jsoup.nodes._
import scala.util.control.Exception._
import scala.collection.JavaConversions._

object Application extends Controller {

  /*
   * Testing code.
   */

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

  //scrapeEverything

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
