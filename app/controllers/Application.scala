package controllers

import play.api._
import play.api.mvc._

import models._


import anorm._
import java.util.Date

import org.jsoup._


object Application extends Controller {

 // val textResults = scrapeGamePage(1)
  
//println(finalPage)

  //println(textResults.filter(x => x.g.steamId == None))

  //val uniRes = textResults.groupBy(_.name).map(_._2.head)(breakOut)

  //scrapeGamePage(1).map(x => println(x.g.steamId))
    //scrapeGamePage(2).map(x => println(x.g.steamId))

  //    scrapeGamePage(1).filter(x => x.g.steamId != None).map(x => Combined.insert(x))

   // scrapeGamePage(2).filter(x => x.g.steamId != None).map(x => Combined.insert(x))
  
  val SS = new SteamScraper(1)
  
  val g1 = SS.scrapePage(SS.gameVals)
  val p1 = SS.scrapePage(SS.priceVals)

  g1 map { Game.insert(_) }
  p1 map { Price.insert(_) }

  //println(g1)
  //println(p1)
  
  def index = Action {
    Ok(views.html.index(List(1,2,3)))
  }

}