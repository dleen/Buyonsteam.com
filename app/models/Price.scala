package models

import java.util.Date
import java.util.Calendar

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

import play.api.libs.json.Json._

case class Price(id: Pk[Long] = NotAssigned,
  priceOnX: Double,
  onSale: Boolean,
  dateRecorded: Date,
  gameId: Int) {

  def setTimeInc(timeMillis: Long): Price = {
    val cal: Calendar = Calendar.getInstance()
    cal.setTime(dateRecorded)
    val curMillis = cal.getTimeInMillis()
    cal.setTimeInMillis(curMillis + timeMillis)
    val d = new Date(cal.getTimeInMillis)

    Price(id, priceOnX, onSale, d, gameId)
  }

}

object Price {

  // Slightly shorter store names
  val nameMap = Map("GreenmanGaming" -> "Greenman",
    "Steam" -> "Steam",
    "DlGamer" -> "DlGamer",
    "GamersGate" -> "GamersG",
    "GameStop" -> "GameStop")

  // Make the price history data for the line graph
  def priceHist(ph: List[GwithP]): String = {

    // Group by store and apply date adjustment to separate similar dates
    val storeGroup = ph.groupBy(_.g.store).mapValues(x => padDates(x))

    // Sort dates from old to new
    val s = storeGroup.mapValues(_.sortBy(y => y.p.dateRecorded))

    // Convert data into json format for the graph
    val priceJson = s.map(x =>
      toJson(Map("name" -> toJson(x._1),
        "data" -> toJson(x._2.map(y => toJson(Map("x" -> toJson(y.p.dateRecorded.getTime),
          "y" -> toJson(y.p.priceOnX))))))))

    // Convert to json and then to a string for preserving double quotes
    toJson(priceJson.toList).toString

  }

  // Function for padding/separating same dates
  // If price has changed x times in one day, assume initial price is recorded at
  // 00:00 hours and add 24/x until midnight
  def padDates(gp: List[GwithP]) = {

    def addHoursToDate(gl: List[GwithP]) = {
      val increment = 1000 * 60 * 60 * 24 / gl.length - 1

      def hoursAdder(d: List[GwithP], acc: List[GwithP], timeinc: Long): List[GwithP] = {
        if (d.isEmpty) acc
        else {
          val gnp: GwithP = GwithP(d.head.g, d.head.p.setTimeInc(timeinc))
          hoursAdder(d.tail, gnp :: acc, timeinc + increment)
        }
      }
      hoursAdder(gl, List(), 0).reverse

    }

    // Group list by dates and order by when the price was entered into db using id
    val dateMap = gp.groupBy(x => x.p.dateRecorded).mapValues(y => y.sortBy(z => z.p.id.get))
    // Apply the padding function and return a flat list
    dateMap.mapValues(x => addHoursToDate(x)).values.toList.flatten

  }

  // Parse a price history from a ResultSet
  val simple = {
    get[Pk[Long]]("price_history.id") ~
      get[Double]("price_history.price_on_x") ~
      get[Boolean]("price_history.on_sale") ~
      get[Date]("price_history.date_recorded") ~
      get[Int]("price_history.game_id") map {
        case id ~ priceOnX ~ onSale ~ dateRecorded ~ gameId =>
          Price(id, priceOnX, onSale, dateRecorded, gameId)
      }
  }

  // Insert a price
  def insert(that: Price, game: Game) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
  			insert into price_history 
  			(price_on_x, on_sale, date_recorded, game_id) 
  			values ({price_on_x}, {on_sale}, {date_recorded}, 
  				(select id from scraped_games where scraped_games.name = {name}
  					and scraped_games.store = {store} ))
  		""").on(
          'price_on_x -> that.priceOnX,
          'on_sale -> that.onSale,
          'date_recorded -> that.dateRecorded,
          'name -> game.name,
          'store -> game.store).executeInsert()
    }
  }

}