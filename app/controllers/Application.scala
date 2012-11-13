package controllers

import play.api._

import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.util.duration.intToDurationInt
import play.api.libs.json.Json._
import play.api.libs.json._
import play.api.mvc.Action
import play.api.mvc.Controller

import scala.collection.JavaConversions._

import scrapers._
import models._
import views._

object Application extends Controller {

  val test = Game.storePrice("dishonored")

  def priceData(name: String) = {
    val gamesP = Game.storePrice(name)
    val games = gamesP map (_._1)

    Action {
    Ok(toJson(
      games map { x =>
        toJson(
          Map("name" -> toJson(x.store),
            "data" -> idPrices(x.id.get),
            "step" -> toJson(true)))
      })
    )}
  }

  val ttest = priceData("dishonored")

  def idPrices(id: Long) = {
    val price = Price.priceById(id) map {
      case (a, b) => List(toJson(a.getTime), toJson(b))
    }
    toJson(price)
  }

  def gameQ(name: String) = Action {
    if (HelperFunctions.listOrSingle(name) == 0) Ok("Nothing found")
    else if (HelperFunctions.listOrSingle(name) == 1) Ok(html.game(Game.storePrice(name)))
    else Ok(html.listgame(Game.findByName(name)))
  }

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
