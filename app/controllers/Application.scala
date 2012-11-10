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



  
  def gameQ(name: String) = Action {
    if (HelperFunctions.listOrSingle(name) == 0) Ok("Nothing found")
    else if (HelperFunctions.listOrSingle(name) == 1) Ok(html.game(Game.storePrice(name)))
    else Ok(html.listgame(Game.findByName(name)))
    }

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
   Action { Home }
  }

  ActorSystem("foo").scheduler.schedule(10.seconds, 1.hour)(scrapeEverything)

  val Home = Redirect(routes.Application.index)
  def index = Action { Ok(html.main(HelperFunctions.recommendGamesA)) }
  
  def gameP(name: String) = Action {
    Ok(html.game(Game.storePrice(name = (name))))
  }
  
  def manualMatching(page: Int) = Action {
    Ok(html.manmatch(DataCleanup.matchManually(page = page)))
  }

  def matchExact = Action {
    Ok(toJson(DataCleanup.matchExactNames))
  }

  def matchSimilar = Action {
    Ok(toJson(DataCleanup.matchSimilarNames))
  }

  def matchem(id1: Int, id2: Int, page: Int) = Action {
    DataCleanup.equateIds(id1, id2)
    Redirect(routes.Application.manualMatching(page))
  }

  def autocompleteSearch(term: String) = Action { Ok(toJson(Game.findPartialName(term))) }

}
