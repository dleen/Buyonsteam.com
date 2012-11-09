package controllers

import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.util.duration.intToDurationInt
import play.api.libs.json.Json.toJson
import play.api.mvc.Action
import play.api.mvc.Controller

import scrapers._
import models._
import views._

object Application extends Controller {

  /*
   * Testing code.
   */

  val recGames = HelperFunctions.recommendGamesA

  def main = Action { Ok(html.main(recGames)) }
  
  def gameP(name: String) = Action { implicit request =>
    Ok(html.game(Game.storePrice(name = (name))))
  }
  
  def gameQ(name: String) = Action { gameP(name) }

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

  def reindex = {
    scrapeEverything
    main
  }

  //ActorSystem("foo").scheduler.schedule(10.seconds, 10.seconds)(test)

  /*
   * Real working code.
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

  def autocompleteSearch(term: String) = Action { Ok(toJson(Game.findPartialName(term))) }

}
