package jobs

import models._
import scrapers._
import akka.actor._

import play.api.Play
import play.api.Mode
import play.api.Application

import java.io.File

object ScrapeJob extends App {

  val application = new Application(new File("."), ScrapeJob.getClass.getClassLoader(), null, Mode.Prod)

  Play.start(application)

  val system = ActorSystem("ScraperSystem")

  val listener = system.actorOf(Props[Listener], name = "listener")

  val GMmaster = system.actorOf(Props(new GreenmanGamingMaster(listener)), name = "GMmaster")
  val GSmaster = system.actorOf(Props(new GameStopMaster(listener)), name = "GSmaster")
  val Dlmaster = system.actorOf(Props(new DlGamerMaster(listener)), name = "Dlmaster")
  val GGmaster = system.actorOf(Props(new GamersGateMaster(listener)), name = "GGmaster")
  val Stmaster = system.actorOf(Props(new SteamMaster(listener)), name = "Stmaster")

  GSmaster ! Scrape
  Dlmaster ! Scrape
  GGmaster ! Scrape
  Stmaster ! Scrape
  GMmaster ! Scrape

}
