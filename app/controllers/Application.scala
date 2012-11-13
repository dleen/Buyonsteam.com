package controllers

import play.api._

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
