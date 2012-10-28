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
  onSale: Boolean,
  gameId: Option[Int])

case class Combined(g: Game, p: Price)

object Combined {

  def insert(c: Combined) = {
    Game.insert(c.g)
    Price.insert(c.p)
  }

}

case class Page[A](items: Seq[A], page: Int, offset: Long, total: Long) {
  lazy val prev = Option(page - 1).filter(_ >= 0)
  lazy val next = Option(page + 1).filter(_ => (offset + items.size) < total)
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

  val withPrice = Game.simple ~ (Price.simple ?) map {
    case game ~ price => (game, price)
  }

  def list(page: Int = 0,
    pageSize: Int = 10,
    orderBy: Int = 1,
    filter: String = "%"): Page[(Game, Option[Price])] = {

    val offset = pageSize * page

    DB.withConnection { implicit connection =>

      val games = SQL(
        """
          select * from games
          left join price_history on game_id = games.id
          where upper(games.name) like upper({filter})
          order by {orderby} nulls last 
          limit {pagesize} offset {offset}  
        """).on(
          'pagesize -> pageSize,
          'offset -> offset,
          'filter -> filter,
          'orderby -> orderBy).as(Game.withPrice *)

      val totalRows = SQL(
        """
          select count(*) from games
          left join price_history on game_id = games.id
          where upper(games.name) like upper({filter})
        """).on(
          'filter -> filter).as(scalar[Long].single)

      Page(games, page, offset, totalRows)

    }
  }

  // Retrieve a game by id
  def findById(id: Long): Option[Game] = {
    DB.withConnection { implicit connection =>
      SQL("select * from games where id = {id}").on('id -> id).as(Game.simple.singleOpt)
    }
  }

  // Retrieve a game by name
  def findByName(name: String): Option[Game] = {
    DB.withConnection { implicit connection =>
      SQL("select * from games where upper(name) like upper({name})")
        .on('name -> name).as(Game.simple.singleOpt)
    }
  }

  // Insert a new game.
  def insert(that: Game) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into games 
          (steam_id, name, url, img_url, release_date, meta_critic)
          values (
          {steam_id}, {name}, {url}, {img_url}, {release_date}, {meta_critic})
        """).on(
          'steam_id -> that.steamId,
          'name -> that.name,
          'url -> that.url,
          'img_url -> that.imgUrl,
          'release_date -> that.releaseDate,
          'meta_critic -> that.metacritic).executeInsert()
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
      get[Boolean]("price_history.on_sale") ~
      get[Option[Int]]("price_history.game_id") map {
        case id ~ name ~ priceOnSteam ~ priceOnAmazon ~ onSale ~ dateRecorded ~ gameId =>
          Price(id, name, priceOnSteam, priceOnAmazon, onSale, dateRecorded, gameId)
      }
  }

  // Find by gameId
  def findById(gameId: Long): List[Price] = {
    DB.withConnection { implicit connection =>
      SQL("select * from price_history where game_id = {game_id}")
        .on('game_id -> gameId).as(Price.simple *)
    }
  }

  // Find by name
  def findByName(name: String): List[Price] = {
    DB.withConnection { implicit connection =>
      SQL("select * from price_history where upper(name) like upper({name})")
        .on('name -> name).as(Price.simple *)
    }
  }

  // Insert a price
  def insert(that: Price) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
	        insert into price_history 
	        (name, price_on_steam, price_on_amazon, on_sale, date_recorded, game_id) 
	        values (
            {name}, {price_on_steam}, {price_on_amazon}, {on_sale}, {date_recorded}, 
	        (select id from games where name = {name}))
	     """).on(
          'name -> that.name,
          'price_on_steam -> that.priceOnSteam,
          'price_on_amazon -> that.priceOnAmazon,
          'on_sale -> that.onSale,
          'date_recorded -> that.dateRecorded).executeInsert()
    }
  }
}


