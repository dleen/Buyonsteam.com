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
              "step" -> toJson(true),
              "id" -> toJson(x.store)))
        }))
    }
  }

  /*
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
  }*/

  def priceDataSymb(name: String) = {
    val gamesP = Game.storePrice(name)
    val gamesPonSale = gamesP.filter(x => x._2.map(_.onSale == true).getOrElse(false))
    val games = gamesPonSale.map(_._1)

    Action {
      Ok(toJson(
        games map { x =>
          toJson(
            Map("type" -> toJson("flags"),
              "data" -> idPricesSymb(x.id.get),
              "onseries" -> toJson(x.store),
              "shape" -> toJson("circlepin")))
        }))
    }
  }

  def idPrices(id: Long) = {
    val price = Price.priceById(id) map {
      case (a, b, true) => Map("x" -> toJson(a.getTime), "y" -> toJson(b),
        "marker" -> toJson(Map("symbol" -> "url(http://www.highcharts.com/demo/gfx/sun.png)")))
      case (a, b, false) => Map("x" -> toJson(a.getTime), "y" -> toJson(b),
        "marker" -> JsNull)
    }
    toJson(price)
  }

  def idPricesSymb(id: Long) = {
    val priceSymb = Price.priceById(id) map {
      case (a, b, false) => JsNull
      case (a, b, true) => toJson(Map("x" -> toJson(a.getTime),
        "title" -> toJson("A"),
        "text" -> toJson("This is a test")))
    }
    toJson(priceSymb)
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

  def autocompleteSearch(term: String) = Action { Ok(toJson(Game.findPartialNameTRI(term))) }

}
