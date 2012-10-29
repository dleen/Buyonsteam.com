package controllers

import play.api._
import play.api.mvc._

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import anorm._

import views._
import models._

import scala.util.control.Exception._

import play.api.libs.json._

import play.api.libs.concurrent._
import play.api.Play.current

import akka.util.Duration
import akka.util.duration._

object Application extends Controller {

  val Home = Redirect(routes.Application.list(0, 2, ""))

  val c1 = SteamScraper(1).getAll

  // Works!! Catches any errors from duplicate values etc.
  
  
  def reindex = Action {
    val promOfIndex: Promise[String] = Akka.future {
       val start: Long = System.currentTimeMillis
      for (i <- (1 to SteamDets.finalPage).par) {
        SteamScraper(i).getAll flatMap (x => catching(classOf[Exception]) opt Combined.insert(x))
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
  
 /* def picalc = Action{
        val tt = ActorForScrape.calculate(nrOfWorkers = 32, nrOfElements = 20000, nrOfMessages = 20000)
        Ok("Hello")
  }*/

  def autocompleteSearch(term: String) = Action { Ok(Json.toJson(Game.findPartialName(term + "%"))) }

  /*
  val tt = c1 flatMap (x => catching(classOf[Exception]) opt Combined.insert(x))
  println(tt)

  println(SteamDets.finalPage)*/

  val testtt = Action {
    println(Game.findPartialName("%" + "dis" + "%"))
    Ok("happy")
  }

  // Input form
  val pageToScrapeForm = Form(
    mapping("storePage" -> number.verifying(min(0), max(50)))(SteamScraper.apply)(SteamScraper.unapply))

  /*
    pageToScrapeForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.index("Ooops, didn't work", formWithErrors, List())),
      x => Ok(html.index("It worked!", pageToScrapeForm, x getAll)))
  } */

  def list(page: Int, orderBy: Int, filter: String) = Action { implicit request =>
    Ok(html.list(Game.list(page = page, orderBy = orderBy, filter = ("%" + filter + "%")), orderBy, filter))
  }

  def index = Action { Home }

}