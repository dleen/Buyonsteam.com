package controllers

import play.api._
import play.api.mvc._

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import views._
import models._

import scala.util.control.Exception._

import play.api.libs.json.Json._

import play.api.libs.concurrent._
import play.api.Play.current

import akka.util.Duration
import akka.util.duration._

import org.postgresql.util._

object Application extends Controller {

  /*
   * Testing code.
   */

  def ggins = Action {
    GamersGateScraper(2).getGames flatMap (x => catching(classOf[PSQLException]) opt Game.insertOtherStore(x))
    Ok("hello")
  }

 /* def reindexOther = Action {
    val promOfIndex: Promise[String] = Akka.future {
      val start: Long = System.currentTimeMillis
      for (i <- (1 to GamersGateDets.finalPage).par) {
        GamersGateScraper(i).getGames flatMap (x => catching(classOf[PSQLException]) opt Game.insertOtherStore(x))
      }
      "Done! " + (System.currentTimeMillis - start).millis.toString
    }
    Async {
      promOfIndex.orTimeout("Oops", 60000).map { eitherIndorTimeout =>
        eitherIndorTimeout.fold(
          timeout => InternalServerError(timeout),
          i => Ok("All " + i))
      }
    }
  }*/

  /*
   * Real working code.
   */

  def reindex = Action {
    val promOfIndex: Promise[String] = Akka.future {
      val start: Long = System.currentTimeMillis
      for (i <- (1 to SteamDets.finalPage).par) {
        SteamScraper(i).getAll flatMap (x => catching(classOf[PSQLException]) opt Combined.insert(x))
      }
      "Done! " + (System.currentTimeMillis - start).millis.toString
    }
    Async {
      promOfIndex.orTimeout("Oops", 20000).map { eitherIndorTimeout =>
        eitherIndorTimeout.fold(
          timeout => InternalServerError(timeout),
          i => Ok("All " + i))
      }
    }
  }

  def autocompleteSearch(term: String) = Action { Ok(toJson(Game.findPartialName(term))) }

  def qlist(name: String) = Action {
    list(0, 2, name)
  }

  def list(page: Int, orderBy: Int, filter: String) = Action { implicit request =>
    Ok(html.list(Game.list(page = page, orderBy = orderBy, filter = ("%" + filter + "%")), orderBy, filter))
  }

  val Home = Redirect(routes.Application.list(0, 2, ""))
  def index = Action { Home }

}