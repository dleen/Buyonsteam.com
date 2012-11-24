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
  imgUrl: String) {

  def shortStoreName(m: Map[String, String]): Game = {
    Game(id, name, m.getOrElse(store, ""), storeUrl, imgUrl)
  }
}

object Game {

  // Slightly shorter store names
  val nameMap = Map("GreenmanGaming" -> "Greenman",
    "Steam" -> "Steam",
    "DlGamer" -> "DlGamer",
    "GamersGate" -> "GamersG",
    "GameStop" -> "GameStop")

  // The most recent game prices from the stores for the BAR CHART
  def mostRecent(gp: List[GwithP]): List[GwithP] = {

    // The dates on which prices were recorded
    val dates = gp map (_.p.dateRecorded.getTime)
    // The most recent dates
    val recentGames = gp filter (_.p.dateRecorded.getTime == dates.max)
    // If more than one price for the most recent date, pick the most recent price
    val rg = recentGames.groupBy(_.g.store).mapValues(_.sortBy(y => -y.p.id.get)).map(_._2.head).toList

    // Order from lowest to highest price and use short store names
    rg.sortBy(_.p.priceOnX).map {
      x => GwithP(x.g.shortStoreName(nameMap), x.p)
    }

  }

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

  val withPrice = Game.simple ~ Price.simple map {
    case game ~ price => GwithP(game, price)
  }

  def storeAllPrice(name: String) = {
    DB.withConnection { implicit connection =>
      SQL("""
        select * from
        (select * from scraped_games
          where scraped_games.unq_game_id = (select distinct on (unq_game_id) unq_game_id from 
            scraped_games where lower(name) = lower({name})) 
      ) as test1
      left join
      price_history on test1.id = game_id
      order by date_recorded desc
      """).on('name -> name).as(withPrice *)
    }
  }

  def storeAllPriceId(id: Int) = {
    DB.withConnection { implicit connection =>
      SQL("""
        select * from
        (select * from scraped_games
          where scraped_games.unq_game_id = {id}
      ) as test1
      left join
      price_history on test1.id = game_id
      order by date_recorded desc
      """).on('id -> id).as(withPrice *)
    }
  }

  // Retrieve a game by id
  def findById(id: Long): Option[Game] = {
    DB.withConnection { implicit connection =>
      SQL("select * from scraped_games where id = {id}").on('id -> id).as(Game.simple.singleOpt)
    }
  }

  // List of distinct games from different stores with this name
  def idByName(name: String) = {
    DB.withConnection { implicit connection =>
      SQL("""
  			select distinct on (id) id from scraped_games
  			where unq_game_id =
  			(select distinct on (unq_game_id) unq_game_id from scraped_games where lower(name) = {name}) 
  			""").on('name -> name).as(scalar[Long] *)
    }
  }

  // Retrieve a game by name
  def findByName(name: String) = {
    DB.withConnection { implicit connection =>
      SQL("""
  			select * from (
  				select distinct on (unq_game_id) *, cast(similarity(name, {name}) as double precision)
  				from scraped_games 
  				where name % {name}
  				order by unq_game_id, id asc) as temp
  		order by similarity desc
  		""")
        .on('name -> name).as(Game.simple ~ get[Double]("similarity") *) map {
          case a ~ b => (a, b)
        }
    }
  }

  // Retrieve a list of matching game names using like method
  def findPartialNameLIKE(name: String): List[String] = {
    DB.withConnection { implicit connection =>
      SQL("""
  			select * from (
  				select distinct on(unq_game_id) name, similarity(name, {name}) from scraped_games
  				where name ilike any (select '%' || {name} || '%' from scraped_games)) as test
  		order by similarity desc
  		limit 6
  		""")
        .on('name -> name).as(str("name") *)
    }
  }

  // Same as above using trigram method
  def findPartialNameTRI(name: String): List[String] = {
    DB.withConnection { implicit connection =>
      SQL("""
  			select * from (
  				select distinct on (unq_game_id) similarity({name}, name), name from scraped_games 
  				where substring(name from 1 for {sz}) % {name}
  				order by unq_game_id, similarity desc
  				limit 6) as sim
  		order by similarity desc
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

}