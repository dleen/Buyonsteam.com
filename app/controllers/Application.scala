package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import anorm._

import views._
import models._

object Application extends Controller {

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