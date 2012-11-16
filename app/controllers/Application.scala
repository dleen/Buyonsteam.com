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

import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.postgresql.util.PSQLException
import java.lang.ExceptionInInitializerError
import java.net.SocketTimeoutException
import java.util.Date

import scala.Option.option2Iterable
import scala.collection.JavaConversions.asScalaBuffer
import scala.util.control.Exception.catching

object Application extends Controller {

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
        }))
    }
  }

  def idPrices(id: Long) = {
    val sale = routes.Assets.at("images/sale.png").toString
    val price = Price.priceById(id) map {
      case (a, b, true) => Map("x" -> toJson(a.getTime), "y" -> toJson(b),
        "marker" -> toJson(Map("symbol" -> toJson("url(" + sale + ")"))))
      case (a, b, false) => Map("x" -> toJson(a.getTime), "y" -> toJson(b),
        "marker" -> JsNull)
    }
    toJson(price)
  }

  def gameQ(name: String) = Action {
    if (HelperFunctions.listOrSingle(name) == 0) Ok("Nothing found")
    else if (HelperFunctions.listOrSingle(name) == 1) Ok(html.game(Game.storePrice(name), PriceStats.game(name)))
    else Ok(html.listgame(Game.findByName(name)))
  }

  val Home = Redirect(routes.Application.index)

  def index = Action { Ok(html.main(HelperFunctions.recommendGamesA)) }

  def gameP(name: String) = Action {
    Ok(html.game(Game.storePrice(name), PriceStats.game(name)))
  }

  def manualMatching(page: Int) = Action {
    Ok(html.manmatch(DataCleanup.matchManually(page = page)))
  }

  def matchem(id1: Int, id2: Int, page: Int) = Action {
    DataCleanup.equateIds(id1, id2)
    Redirect(routes.Application.manualMatching(page))
  }

  def autocompleteSearch(term: String) = Action { Ok(toJson(Game.findPartialNameTRI(term))) }

}
