package models

import java.util.Date

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class Game(id: Pk[Long] = NotAssigned,
  name: String,
  store: String,
  storeUrl: String,
  imgUrl: String)

case class SteamGame(steamId: Int,
  name: String,
  releaseDate: String,
  metacritic: Int,
  gameId: Int)

case class Price(id: Pk[Long] = NotAssigned,
  priceOnX: Double,
  onSale: Boolean,
  dateRecorded: Date,
  gameId: Int)

case class GwithP(g: Game, p: Price)

case class GSwP(sg: SteamGame, gwp: GwithP)

object GSwP {

  def insert(x: GSwP) = {
    GwithP.insert(x.gwp)
    SteamGame.insert(x.sg)
  }
}

object GwithP {

  def insert(c: GwithP) = {
    Game.insert(c.g)
    Price.insert(c.p, c.g)
  }

  def priceInsert(c: GwithP) = {
    Price.insert(c.p, c.g)
  }

}

case class Page[A](items: Seq[A], page: Int, offset: Long, total: Long) {
  lazy val prev = Option(page - 1).filter(_ >= 0)
  lazy val next = Option(page + 1).filter(_ => (offset + items.size) < total)
}

object Game {

  // Parse a game from a ResultSet
  val simple = {
    get[Pk[Long]]("scraped_games.id") ~
      get[String]("scraped_games.name") ~
      get[String]("scraped_games.store") ~
      get[String]("scraped_games.store_url") ~
      get[String]("scraped_games.img_url") map {
        case id ~ name ~ store ~ storeUrl ~ imgUrl =>
          Game(id, name, store, storeUrl, imgUrl)
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

      val gWithAllP = SQL(
        """
          select * from scraped_games
          left join price_history on game_id = scraped_games.id
          where (levenshtein(lower(substring(scraped_games.name from 1 for {sz})),
          lower({filter})) < 3)
          order by {orderby}
          limit {pagesize} offset {offset}  
        """).on(
          'pagesize -> pageSize,
          'offset -> offset,
          'filter -> filter,
          'orderby -> orderBy,
          'sz -> (filter.length + 1)).as(Game.withPrice *)

      val totalRows = SQL(
        """
          select count(*) from scraped_games
          left join price_history on game_id = scraped_games.id
          where (levenshtein(lower(substring(scraped_games.name from 1 for {sz})),
          lower({filter})) < 3)
        """).on(
          'filter -> filter, 'sz -> (filter.length + 1)).as(scalar[Long].single)

      Page(gWithAllP, page, offset, totalRows)

    }
  }

  // Retrieve a game by id
  def findById(id: Long): Option[Game] = {
    DB.withConnection { implicit connection =>
      SQL("select * from games where id = {id}").on('id -> id).as(Game.simple.singleOpt)
    }
  }

  // Retrieve a game by name
  // FIX THIS
  def findByName(name: String): Option[Game] = {
    DB.withConnection { implicit connection =>
      SQL("select * from games where upper(name) like upper({name})")
        .on('name -> name).as(Game.simple.singleOpt)
    }
  }

  // Retrieve a list of matching game names
  def findPartialName(name: String): List[String] = {
    DB.withConnection { implicit connection =>
      SQL("""
          select name from scraped_games 
          where (levenshtein(lower(substring(name from 1 for {sz})), lower({name})) < 3)
          limit 6
          """)
        .on('name -> name, 'sz -> (name.length + 1)).as(str("name") *)
    }
  }

  // Insert a new game.
  def insert(that: Game) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into scraped_games 
          (name, store, store_url, img_url)
          values ({name}, {store}, {store_url}, {img_url})
        """).on(
          'name -> that.name,
          'store -> that.store,
          'store_url -> that.storeUrl,
          'img_url -> that.imgUrl).executeInsert()
    }
  }

  // NOT FINISHED
  // IDEA IS TO UPDATE UNQ_GAME_ID TO BE THE
  // SAME FOR GAMES WITH SIMILAR ENOUGH NAMES
  def potentialSimNames = {
    DB.withConnection { implicit connection =>
      SQL(
        """
    	  select scraped_games.name from 
    	  scraped_games join steam_games 
    	  on (scraped_games.name = steam_games.name and scraped_games.store != 'Steam')
    	 """)
    }
  }

}

object SteamGame {

  val simple = {
    get[Int]("steam_games.steam_id") ~
      get[String]("steam_games.name") ~
      get[String]("steam_games.release_date") ~
      get[Int]("steam_games.meta_critic") ~
      get[Int]("steam_games.game_id") map {
        case steamId ~ name ~ releaseDate ~ metacritic ~ gameId =>
          SteamGame(steamId, name, releaseDate, metacritic, gameId)
      }
  }

  def insert(that: SteamGame) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
	       insert into steam_games
	       (steam_id, name, release_date, meta_critic, game_id) 
	       values ({steam_id}, {name}, {release_date}, {meta_critic}, 
	       (select id from scraped_games where scraped_games.name = {name}
    	   and scraped_games.store = {store} ))
	    """).on(
          'steam_id -> that.steamId,
          'name -> that.name,
          'release_date -> that.releaseDate,
          'meta_critic -> that.metacritic,
          'store -> "Steam").executeInsert()
    }
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

  // Find by gameId
  def findByGameId(gameId: Long): List[Price] = {
    DB.withConnection { implicit connection =>
      SQL("select * from price_history where game_id = {game_id}")
        .on('game_id -> gameId).as(Price.simple *)
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


