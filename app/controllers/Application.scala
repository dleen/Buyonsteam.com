package controllers

import play.api._
import play.api.mvc._

import models._
import models.steamScraper._

import anorm._
import java.util.Date

object Application extends Controller {

  val textResults = scrapeGamePage(1)
  //println(textResults)

  //println(textResults(1))
  //println(textResults(1) == None)

  val testins = Game(NotAssigned, Some(5), "Hello", "World", Some("Test"), Some(new Date()), Some(44))

  Game.insert(textResults(1))

  def index = Action {
    Ok(views.html.index(textResults.toList))
  }

}