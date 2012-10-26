package controllers

import play.api._
import play.api.mvc._

import models._

import anorm._
import java.util.Date

import org.jsoup._
import scala.collection.JavaConversions._

object Application extends Controller {

  val SS = new SteamScraper(1)

  val g1 = SS.scrapePage(SS.gameVals)
  val p1 = SS.scrapePage(SS.priceVals)

  val SQ = new SteamScraper(2)

  val c2 = SQ.scrapePage(SQ.allVals)

  g1 map { Game.insert(_) }
  p1 map { Price.insert(_) }

  c2 map { Combined.insert(_) }

  //println(g1)
  //println(p1)
  //println(c2)

  def index = Action {
    Ok(views.html.index(List(1, 2, 3)))
  }

}