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

object Application extends Controller {

  val c1 = SteamScraper(1).getAll

  // Stops on first error.
  try {
    c1 map { Combined.insert(_) }
  } catch {
    case e => println("oops")
  }

  // Works!! Catches any errors from duplicate values etc.
  for (i <- 1 to SteamDets.finalPage) {
    SteamScraper(i).getAll flatMap (x => catching(classOf[Exception]) opt Combined.insert(x))
  }
  
  val tt = c1 flatMap (x => catching(classOf[Exception]) opt Combined.insert(x))
  println(tt)

  println(SteamDets.finalPage)
  
  // Input form
  val pageToScrapeForm = Form(
    mapping("storePage" -> number.verifying(min(0), max(50)))(SteamScraper.apply)(SteamScraper.unapply))

  def scrape = Action { implicit request =>
    pageToScrapeForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.index("Ooops, didn't work", formWithErrors, List())),
      x => Ok(html.index("It worked!", pageToScrapeForm, x getAll)))
  }

  def index = Action {
    Ok(html.index("Hello", pageToScrapeForm, List()))
  }

}