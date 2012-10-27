package models

import java.util.Date

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class Game(id: Pk[Long] = NotAssigned,
  steamId: Option[Int],
  name: String, url: String,
  imgUrl: String,
  releaseDate: String,
  metacritic: Option[Int])

case class Price(id: Pk[Long] = NotAssigned,
  name: String,
  priceOnSteam: Option[Double],
  priceOnAmazon: Option[Double],
  dateRecorded: Date,
  gameId: Option[Int])

case class Combined(g: Game, p: Price)

object Combined {
  def insert(c: Combined) = {
    Game.insert(c.g)
    Price.insert(c.p)
  }
}

object Game {

  // Parse a game from a ResultSet
  val simple = {
    get[Pk[Long]]("games.id") ~
      get[Option[Int]]("games.steam_id") ~
      get[String]("games.name") ~
      get[String]("games.url") ~
      get[String]("games.img_url") ~
      get[String]("games.release_date") ~
      get[Option[Int]]("games.meta_critic") map {
        case id ~ steamId ~ name ~ url ~ imgUrl ~ releaseDate ~ metacritic =>
          Game(id, steamId, name, url, imgUrl, releaseDate, metacritic)
      }
  }

  // Retrieve a game by name

  // Retrieve a game by id

  // Insert a new game.
  def insert(thatGame: Game) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into games 
          (steam_id, name, url, img_url, release_date, meta_critic)
          values (
          {steam_id}, {name}, {url}, {img_url}, {release_date}, {meta_critic})
        """).on(
          'steam_id -> thatGame.steamId,
          'name -> thatGame.name,
          'url -> thatGame.url,
          'img_url -> thatGame.imgUrl,
          'release_date -> thatGame.releaseDate,
          'meta_critic -> thatGame.metacritic).executeInsert()
    }
  }

}

object Price {

  // Parse a price history from a ResultSet
  val simple = {
    get[Pk[Long]]("price_history.id") ~
      get[String]("price_history.name") ~
      get[Option[Double]]("price_history.price_on_steam") ~
      get[Option[Double]]("price_history.price_on_amazon") ~
      get[Date]("price_history.date_recorded") ~
      get[Option[Int]]("price_history.game_id") map {
        case id ~ name ~ priceOnSteam ~ priceOnAmazon ~ dateRecorded ~ gameId =>
          Price(id, name, priceOnSteam, priceOnAmazon, dateRecorded, gameId)
      }
  }

  // Insert a price date pair
  def insert(thatHistory: Price) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
	        insert into price_history 
	        (name, price_on_steam, price_on_amazon, date_recorded, game_id) 
	        values (
            {name}, {price_on_steam}, {price_on_amazon}, {date_recorded}, 
	        (select id from games where name = {name})
	        )
	     """).on(
          'name -> thatHistory.name,
          'price_on_steam -> thatHistory.priceOnSteam,
          'price_on_amazon -> thatHistory.priceOnAmazon,
          'date_recorded -> thatHistory.dateRecorded).executeUpdate()
    }
  }
}


