package controllers

import play.api.libs.json.Json._

import play.api.mvc.Action
import play.api.mvc.Controller

import models._
import views._

import java.util.Date


object Application extends Controller {
  
  def sample = Action{ Ok(html.sample()) }

  type datePrice = (java.util.Date, Double, Boolean)

  // Slightly shorter store names
  val nameMap = Map("GreenmanGaming" -> "Greenman",
    "Steam" -> "Steam",
    "DlGamer" -> "DlGamer",
    "GamersGate" -> "GamersG",
    "GameStop" -> "GameStop")

  // The most recent game prices from the stores for the BAR CHART
  def mostRecent(gp: List[GwithP]): List[GwithP] = {

    // The dates on which prices were recorded
    val dates = gp map(_.p.dateRecorded.getTime)
    // The most recent dates
    val recentGames = gp filter(_.p.dateRecorded.getTime == dates.max)
    // If more than one price for the most recent date, pick the most recent price
    val rg = recentGames.groupBy(_.g.store).mapValues(_.sortBy(y => -y.p.id.get)).map(_._2.head).toList

    // Order from lowest to highest price and use short store names
    rg.sortBy(_.p.priceOnX).map {
      x => GwithP(x.g.shortStoreName(nameMap), x.p)
    }

  }

  // Make the price history data for the line graph
  def priceHist(ph: List[GwithP]): String = {

    // Group by store and apply date adjustment to separate similar dates
    val storeGroup = ph.groupBy(_.g.store).mapValues(x => padDates(x))

    // Sort dates from old to new
    val s = storeGroup.mapValues(_.sortBy(y => y.p.dateRecorded))

    // Convert data into json format for the graph
    val priceJson = s.map(x =>
     toJson(Map("name" -> toJson(x._1),
      "data" -> toJson(x._2.map(y => toJson(Map("x" -> toJson(y.p.dateRecorded.getTime),
       "y" -> toJson(y.p.priceOnX)))))))
     )

    // Convert to json and then to a string for preserving double quotes
    toJson(priceJson.toList).toString

  }

  // Function for padding/separating same dates
  // If price has changed x times in one day, assume initial price is recorded at
  // 00:00 hours and add 24/x until midnight
  def padDates(gp: List[GwithP]) = {

    def addHoursToDate(gl: List[GwithP]) = {
      val increment = 1000 * 60 * 60 * 24 / gl.length - 1

      def hoursAdder(d: List[GwithP], acc: List[GwithP], timeinc: Long): List[GwithP] = {
        if (d.isEmpty) acc
        else {
          val gnp: GwithP = GwithP(d.head.g, d.head.p.setTimeInc(timeinc))
          hoursAdder(d.tail, gnp :: acc, timeinc + increment)
        }
      }
      hoursAdder(gl, List(), 0).reverse

    }

    // Group list by dates and order by when the price was entered into db using id
    val dateMap = gp.groupBy(x => x.p.dateRecorded).mapValues(y => y.sortBy(z => z.p.id.get))
    // Apply the padding function and return a flat list
    dateMap.mapValues(x => addHoursToDate(x)).values.toList.flatten

  }


  // Search listings page
  def gameQ(name: String) = {
    if (HelperFunctions.listOrSingle(name) == 0) Action { Ok("Nothing found") }
    else if (HelperFunctions.listOrSingle(name) == 1 && !Game.storeAllPrice(name).isEmpty) Action { gameP(name) }
    else Action{ Ok(html.listgame(Game.findByName(name))) }
    }

  // Index/homepage
  val Home = Redirect(routes.Application.index)

  def index = Action { Ok(html.main(HelperFunctions.recommendGamesA)) }

  // Main game results/graph page
  def gameP(name: String) = Action {
    val g = Game.storeAllPrice(name)

    if (g.isEmpty) Ok("Error")
    else Ok(html.game(mostRecent(g), priceHist(g), PriceStats.game(name).map(_.shortStoreName(nameMap))))
  }

  // Manually matching duplicate names
  def manualMatching(page: Int) = Action {
    Ok(html.manmatch(DataCleanup.matchManually(page)))
  }

  def matchem(id1: Int, id2: Int, page: Int) = Action {
    DataCleanup.equateIds(id1, id2)
    Redirect(routes.Application.manualMatching(page))
  }

  // Autocomplete
  def autocompleteSearch(term: String) = Action { Ok(toJson(Game.findPartialNameTRI(term))) }

}
