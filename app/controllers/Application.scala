package controllers

import play.api.libs.json.Json._

import play.api.mvc.Action
import play.api.mvc.Controller

import play.api.data._
import play.api.data.Forms._

import models._
import models.Game._
import models.Price._
import views._

import java.util.Date

object Application extends Controller {
  
  // Comparison graph of deals across stores
  def compData(sc: List[StoreComparison]) = {
    val sg = sc groupBy (_.store) mapValues (x => x.sortBy(_.dateRecorded))
    val s2 = sg.map(x =>
      toJson(Map("name" -> toJson(x._1), 
          "data" -> toJson(x._2.map(y => 
            toJson(Map("x" -> toJson(y.dateRecorded.getTime),
                "y" -> toJson(y.count)))))))).toList
    
      toJson(s2).toString          
  }

  def sc = Action { Ok(html.storecomp(compData(StoreComparison.timeLine))) }

  // Sample layout game page. Temporary.
  def sample(name: String) = {
    val g = Game.storeAllPrice(name)
    Action { Ok(html.sample(mostRecent(g), priceHist(g), PriceStats.game(name).map(_.shortStoreName(nameMap)))) }
  }

  // Index/homepage
  val Home = Redirect(routes.Application.index)

  def index = Action { Ok(html.main(HelperFunctions.recommendGamesA)) }

  /**
   * ******************************
   * Main game results/graph page.
   * ******************************
   */

  // Route: game
  def gameR(name: String) = gameQ(name)

  // Search listings page. Route: /:name
  def gameQ(name: String) = {
    val avail = SearchResult.singleOrList(name) sortBy (x => -x.sim)
    val exact = avail filter (x => ((1 - x.sim) < 0.01 || x.cnt == 1))

    if (avail.isEmpty) Action { Ok("Nothing found gameQ") }
    else if (exact.length == 1) {
      Action {
        val g = Game.storeAllPrice(exact.head.g.g.name)

        if (g.isEmpty) Ok("Error")
        else Ok(html.game(mostRecent(g), priceHist(g), 
          PriceStats.game(exact.head.g.g.name).map(_.shortStoreName(nameMap)), 
          avail.tail.take(5)))
      }
    } else Action {
      val g = Game.storeAllPrice(avail.head.g.g.name)

      if (g.isEmpty) Ok("Error")
      else Ok(html.game(mostRecent(g), priceHist(g), 
        PriceStats.game(avail.head.g.g.name).map(_.shortStoreName(nameMap)), 
        avail.tail.take(5)))
    }
  }

  /**
   * **************
   * Data Cleanup
   * **************
   */

   val testForm = Form(
    mapping(
    "selectedTerm" -> list(text)
    )(MatchedIds.apply)(MatchedIds.unapply)
    )

  // Manually matching duplicate names
  def manualMatching(page: Int, filter: String) = Action {
    Ok(html.manmatch(DataCleanup.matchManually(page, filter = (filter)), filter, testForm))
  }

  def matchem(page: Int, filter: String) = Action { implicit request =>
    val selected = testForm.bindFromRequest.get
    val twp = selected.selectedTerm.map(x => x.split(',').toList)
    twp.map(x => DataCleanup.equateIds(x(0).toInt, x(1).toInt))
    Redirect(routes.Application.manualMatching(page, filter))
  }

  // Autocomplete
  def autocompleteSearch(term: String) = Action { Ok(toJson(Game.findPartialNameTRI(term))) }

  // Slightly shorter store names
  val nameMap = Map("GreenmanGaming" -> "Greenman",
    "Steam" -> "Steam",
    "DlGamer" -> "DlGamer",
    "GamersGate" -> "GamersG",
    "GameStop" -> "GameStop")

}
