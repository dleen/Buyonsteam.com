package models

import java.util.Date

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class Game(id: Pk[Long] = NotAssigned, steamId: Option[Int],
  gameName: String, url: String, imgUrl: String,
  releaseDate: String, metaCritic: Option[Int])

case class PriceHistory(id: Pk[Long] = NotAssigned, gameName: String,
  priceOnSteam: Option[Double], priceOnAmazon: Option[Double],
  dateRecorded: Date)

object Game {

  // Parse a game from a ResultSet
  // TODO

  // Retrieve a game by name

  // Retrieve a game by id

  // Insert a new game.
  def insert(thisGame: Game) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into games 
          (steam_id, game_name, url, img_url, release_date, meta_critic)
          values (
          {steam_id}, {game_name}, {url}, {img_url}, {release_date}, {meta_critic}
          )
        """).on(
          'steam_id -> thisGame.steamId,
          'game_name -> thisGame.gameName,
          'url -> thisGame.url,
          'img_url -> thisGame.imgUrl,
          'release_date -> thisGame.releaseDate,
          'meta_critic -> thisGame.metaCritic).executeUpdate()
    }
  }

}

object PriceHistory {

  // Insert a price date pair
  def insert(thisHistory: PriceHistory) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
	        insert into price_history 
	        (price_on_steam, price_on_amazon, date_recorded, game_id) 
	        values (
            {game_name}, {price_on_steam}, {price_on_amazon}, {date_recorded}, 
	        (select id from games where game_name = {game_name})
	        )
	     """).on(
          'price_on_steam -> thisHistory.priceOnSteam,
          'price_on_amazon -> thisHistory.priceOnAmazon,
          'date_recorded -> new Date(),
          'game_name -> thisHistory.gameName).executeUpdate()
    }
  }
}


