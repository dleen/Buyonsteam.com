package controllers

import play.api._
import play.api.mvc._

import models._
import models.steamScraper._

import anorm._
import java.util.Date

import collection.breakOut

object Application extends Controller {

  val textResults = scrapeGamePage(1)
  
  val uniRes = textResults.groupBy(_.name).map(_._2.head)(breakOut)
  
  println(textResults.length)
  println(uniRes.length)
  //println(uniRes)

  //uniRes map { Game.insert(_) }

  //textResults map { Game.insert(_) }
  
    //val testins = Game(NotAssigned, Some(89), "Hello3", "World", "Test", "Date", Some(44))
    //val histins = PriceHistory(NotAssigned, "Hello3", Some(59.99), None, new Date(), None)
    //Game.insert(testins)
    //PriceHistory.insert(histins)

  def index = Action {
    Ok(views.html.index((scrapeGamePage(2).toList ++ scrapeGamePage(4).toList)))
  }

}