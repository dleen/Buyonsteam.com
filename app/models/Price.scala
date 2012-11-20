package models

import java.util.Date
import java.util.Calendar

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._


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