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

  println(textResults.filter(x => x.g.steamId == None))

  //val uniRes = textResults.groupBy(_.name).map(_._2.head)(breakOut)

  scrapeGamePage(1).map(x => println(x.g.steamId))
    scrapeGamePage(2).map(x => println(x.g.steamId))

  //    scrapeGamePage(1).filter(x => x.g.steamId != None).map(x => Combined.insert(x))

   // scrapeGamePage(2).filter(x => x.g.steamId != None).map(x => Combined.insert(x))
  
  def index = Action {
    Ok(views.html.index((scrapeGamePage(1))))
  }

}