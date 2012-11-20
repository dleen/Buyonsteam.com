package jobs

import java.io.File

import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import play.api.Application
import play.api.Mode
import play.api.Play
import scrapers.DlGamerMaster
import scrapers.GameStopMaster
import scrapers.GamersGateMaster
import scrapers.GreenmanGamingMaster
import scrapers.Listener
import scrapers.Scrape
import scrapers.SteamMaster
import scrapers.SteamDLCMaster
import scrapers.SteamPkMaster

object ScrapeJobGS extends App {

  val application = new Application(new File("."), ScrapeJobGS.getClass.getClassLoader(), null, Mode.Prod)
  Play.start(application)

  val system = ActorSystem("ScraperSystem")
  val listener = system.actorOf(Props(new Listener(1)), name = "listener")

  val GSmaster = system.actorOf(Props(new GameStopMaster(listener)), name = "GSmaster")

  GSmaster ! Scrape

}

object ScrapeJobGM extends App {

  val application = new Application(new File("."), ScrapeJobGM.getClass.getClassLoader(), null, Mode.Prod)
  Play.start(application)

  val system = ActorSystem("ScraperSystem")
  val listener = system.actorOf(Props(new Listener(1)), name = "listener")

  val GMmaster = system.actorOf(Props(new GreenmanGamingMaster(listener)), name = "GMmaster")

  GMmaster ! Scrape

}

object ScrapeJobDl extends App {

  val application = new Application(new File("."), ScrapeJobDl.getClass.getClassLoader(), null, Mode.Prod)
  Play.start(application)

  val system = ActorSystem("ScraperSystem")
  val listener = system.actorOf(Props(new Listener(1)), name = "listener")

  val Dlmaster = system.actorOf(Props(new DlGamerMaster(listener)), name = "Dlmaster")

  Dlmaster ! Scrape

}

object ScrapeJobSt extends App {

  val application = new Application(new File("."), ScrapeJobSt.getClass.getClassLoader(), null, Mode.Prod)
  Play.start(application)

  val system = ActorSystem("ScraperSystem")
  val listener = system.actorOf(Props(new Listener(1)), name = "listener")

  val Stmaster = system.actorOf(Props(new SteamMaster(listener)), name = "Stmaster")

  Stmaster ! Scrape

}

object ScrapeJobStDLC extends App {

  val application = new Application(new File("."), ScrapeJobStDLC.getClass.getClassLoader(), null, Mode.Prod)
  Play.start(application)

  val system = ActorSystem("ScraperSystem")
  val listener = system.actorOf(Props(new Listener(1)), name = "listener")

  val StDLCmaster = system.actorOf(Props(new SteamDLCMaster(listener)), name = "StDLCmaster")

  StDLCmaster ! Scrape

}

object ScrapeJobStPk extends App {

  val application = new Application(new File("."), ScrapeJobStPk.getClass.getClassLoader(), null, Mode.Prod)
  Play.start(application)

  val system = ActorSystem("ScraperSystem")
  val listener = system.actorOf(Props(new Listener(1)), name = "listener")

  val StPkmaster = system.actorOf(Props(new SteamPkMaster(listener)), name = "StPkmaster")

  StPkmaster ! Scrape

}

object ScrapeJobGG extends App {

  val application = new Application(new File("."), ScrapeJobGG.getClass.getClassLoader(), null, Mode.Prod)
  Play.start(application)

  val system = ActorSystem("ScraperSystem")
  val listener = system.actorOf(Props(new Listener(1)), name = "listener")

  val GGmaster = system.actorOf(Props(new GamersGateMaster(listener)), name = "GGmaster")

  GGmaster ! Scrape

}