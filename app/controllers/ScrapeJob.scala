package controllers

import models._
import scrapers._
import akka.actor._


object ScrapeJob extends App {

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