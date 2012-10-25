package controllers

import play.api._
import play.api.mvc._

import models._
import models.steamScraper._

import anorm._
import java.util.Date

object Application extends Controller {

  val textResults = scrapeGamePage(5)
  println(textResults)

  //println(textResults(1))
  //println(textResults(1) == None)

  val testins = Game(NotAssigned, Some(5), "Hello", "World", "Test", "Date", Some(44))

  // Fall 2012
  Game.insert(textResults(11))

  def index = Action {
    Ok(views.html.index(textResults.toList))
  }

}