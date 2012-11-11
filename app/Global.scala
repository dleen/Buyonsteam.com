import play.api._

import akka.actor._

import scrapers._

import akka.util.duration.intToDurationInt

object Global extends GlobalSettings {



  override def onStart(app: Application) {

   // SS.scheduler.schedule(30.seconds, 1.hours, Runner, Gogo)
    //SS.scheduler.schedule(10.seconds, 30.seconds, Runner, Gogo1)

  }

  override def onStop(app: Application) {
    //SS.shutdown
  }

}