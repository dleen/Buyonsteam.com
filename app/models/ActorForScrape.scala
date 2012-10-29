package models
/*
import akka.actor._

import akka.routing.RoundRobinRouter
import akka.util.Duration
import akka.util.duration._

import scala.util.control.Exception._


object ActorForScrape {

  sealed trait Pimessage
  case object Calculate extends Pimessage
  case class Work(page: Int) extends Pimessage
  case class Result(value: Double) extends Pimessage
  case class PiApproximation(pi: Double, duration: Duration)

  class Worker extends Actor {

    def receive = {
      case Work(page) =>
        sender ! Result(SteamScraper(page).getAll flatMap (x => catching(classOf[Exception]) opt Combined.insert(x)))
    }
  }

  class Master(nrOfWorkers: Int, nrOfMessages: Int, nrOfElements: Int, listener: ActorRef) extends Actor {
    var pi: Double = _
    var nrOfResults: Int = _
    val start: Long = System.currentTimeMillis

    val workerRouter = context.actorOf(
      Props[Worker].withRouter(RoundRobinRouter(nrOfWorkers)), name = "workerRouter")

    def receive = {
      case Scrape =>
        for (i <- 1 until SteamDets.finalPage) workerRouter ! Work(i * nrOfElements, nrOfElements)
      case Result(value) =>
        pi += value
        nrOfResults += 1
        if (nrOfResults == nrOfMessages) {
          listener ! PiApproximation(pi, duration = (System.currentTimeMillis - start).millis)
          context.stop(self)
        }
    }
  }

  class Listener extends Actor {
    def receive = {
      case PiApproximation(pi, duration) =>
        println("\n\tPi approximation: \t\t%s\n\tCalculation time: \t%s"
          .format(pi, duration))
        context.system.shutdown()
    }
  }

  def calculatePiFor(start: Int, nrOfElements: Int): Double = {
    var acc = 0.0
    for (i <- start until (start + nrOfElements))
      acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1)
    acc
  }

  def calculate(nrOfWorkers: Int, nrOfElements: Int, nrOfMessages: Int) = {
    val system = ActorSystem("PiSystem")

    val listener = system.actorOf(Props[Listener], name = "Listener")

    val master = system.actorOf(Props(new Master(
      nrOfWorkers, nrOfMessages, nrOfElements, listener)), name = "master")

    master ! Calculate
  }

}*/