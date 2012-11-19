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

  def insertGame(x: GSwP) = {
    GwithP.insertGame(x.gwp)
  }

  def insertPrice(x: GSwP) = {
    GwithP.insertPrice(x.gwp)
  }

  def insertSteam(x: GSwP) = {
    SteamGame.insert(x.sg)
  }

}

object GwithP {

  def insertGame(c: GwithP) = {
    Game.insert(c.g)
  }

  def insertPrice(c: GwithP) = {
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

  val withPrice = Game.simple ~ Price.simple map {
    case game ~ price => (game, price)
  }

  def list(page: Int = 0,
    pageSize: Int = 10,
    orderBy: Int = 1,
    filter: String = "%"): Page[(Game, Price)] = {

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

  def storePrice(name: String) = {
    DB.withConnection { implicit connection =>
      SQL("""
        select distinct on(s1) * from (
          select * from
          (select store as s1, * from scraped_games
            where scraped_games.unq_game_id = (select distinct on (unq_game_id) unq_game_id from 
              scraped_games where lower(name) = lower({name})) 
      ) as test1
      left join
      (select * from price_history) as test2
      on test1.id = test2.game_id
      ) as test3
      order by s1, date_recorded desc
      """).on('name -> name).as(withPrice *)

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

    def storeById(id: Int) = {
      DB.withConnection { implicit connection =>
        SQL(""" 
         select store from scraped_games where id = {id}
         order by store
         """).on('id -> id).as(scalar[String].single)
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

  def priceById(id: Long) = {
    DB.withConnection { implicit connection =>
      SQL("""     
       select date_recorded, price_on_x, on_sale from price_history where game_id = {id}
       order by date_recorded asc, id asc
       """).on('id -> id).as(get[Date]("date_recorded") ~
       get[Double]("price_on_x") ~ 
       get[Boolean]("on_sale") *).map {
        case a ~ b ~ c => (a, b, c)
      }
    }
  }
}

case class DataCleanup(sim: Double, n1: String, id1: Int, url1: String,
  n2: String, id2: Int, url2: String)

object DataCleanup {

  def matchExactNames = {
    DB.withConnection { implicit connection =>
      SQL(
        """
        with pairs (a, b) as
        (select n1.unq_game_id, n2.unq_game_id
          from scraped_games n1
          join scraped_games n2 on lower(n1.name) = lower(n2.name) AND n1.store <> n2.store 
          AND n1.unq_game_id <> n2.unq_game_id
          where n1.unq_game_id < n2.unq_game_id
          order by n1.unq_game_id)
      update scraped_games set unq_game_id = a from pairs where unq_game_id = b  
      """).executeUpdate()
    }
  }

  def matchSimilarNames = {
    DB.withConnection { implicit connection =>
      SQL(
        """
        with matched(b, c) as (
         SELECT n1.unq_game_id, n2.unq_game_id
         FROM   scraped_games n1
         JOIN   scraped_games n2 ON n1.unq_game_id <> n2.unq_game_id AND n1.store != n2.store 
         AND lower(substring(n1.name from 1 for 3)) = lower(substring(n2.name from 1 for 3))
         where n1.unq_game_id < n2.unq_game_id and similarity(n1.name, n2.name) = 1)
      update scraped_games set unq_game_id = b from matched where unq_game_id = c
      """).executeUpdate()
    }
  }

  val simple = {
    get[Double]("a") ~
    get[String]("b") ~
    get[Int]("c") ~
    get[String]("d") ~
    get[String]("e") ~
    get[Int]("f") ~
    get[String]("g") map {
      case sim ~ n1 ~ url1 ~ id1 ~ n2 ~ id2 ~ url2 =>
      DataCleanup(sim, n1, url1, id1, n2, id2, url2)
    }
  }

  def matchManually(page: Int = 0, pageSize: Int = 10) = {

    val offset = pageSize * page

    val matchedPairs = DB.withConnection { implicit connection =>
      SQL(
        """
        with matched(a,b,c,d,e,f,g) as (
          SELECT distinct on(n1.unq_game_id,n2.unq_game_id) cast(similarity(n1.name, n2.name) as double precision), 
          n1.name, n1.unq_game_id, n1.store_url, n2.name, n2.unq_game_id, n2.store_url
          FROM   scraped_games n1
          JOIN   scraped_games n2 ON n1.unq_game_id < n2.unq_game_id AND n1.store != n2.store 
          AND lower(substring(n1.name from 1 for 3)) = lower(substring(n2.name from 1 for 3))
          order by n1.unq_game_id, n2.unq_game_id, n1.name, n2.name )
      select *, count(*) over() as full_count
      from matched
      order by a desc, b, c
      limit {pagesize} offset {offset}
      """).on('pagesize -> pageSize, 'offset -> offset)
      .as(DataCleanup.simple ~ get[Long]("full_count") *) map {
        case a ~ b => (a, b)
      }
    }

    Page(matchedPairs map { case (a, b) => a },
      page, offset,
      matchedPairs map { case (a, b) => b } head)
  }

  def equateIds(id1: Int, id2: Int) = {
    DB.withConnection { implicit connection =>
      SQL("""
        update scraped_games set unq_game_id = {id1} where unq_game_id = {id2}
        """).on('id1 -> id1, 'id2 -> id2).executeUpdate()
    }
  }

}

object HelperFunctions {

  val withSteamPrice = Game.simple ~ (SteamGame.simple ?) ~ Price.simple map {
    case game ~ steam ~ price => (game, steam, price)
  }

  def recommendGamesA = {
    DB.withConnection { implicit connection =>
      SQL("""
       select * from (
         select distinct on (unq_game_id) * from(
           (select * from scraped_games where store = 'Steam') as s1
           left join price_history p1 on s1.id = p1.game_id
           left join steam_games on s1.id = steam_games.game_id)
      where date_recorded = 'today'
      order by unq_game_id, p1.id desc) as sg
      where on_sale = true
      order by meta_critic desc
      """).as(withSteamPrice *)
    }
  }

  // Order by discount amount instead of metacritic
  // TODO
  def recommendGamesB = {}

  def listOrSingle(name: String) = {
    DB.withConnection { implicit connection =>
      SQL("""
        select count(*) from (
          select distinct on (unq_game_id) id
          from scraped_games
          where name % {name}) as temp
      """).on('name -> name)
      .as(scalar[Long].single).toInt
    }
  }

}

case class PriceStats(maxD: Date, 
  max: Double, 
  smax: String, 
  minD: Date, 
  min: Double, 
  smin: String, 
  avg: Double)

object PriceStats {

  val simple = {
    get[Date]("d1") ~
    get[Double]("max") ~
    get[String]("s1") ~
    get[Date]("d2") ~
    get[Double]("min") ~
    get[String]("s2") ~
    get[Double]("avg") map {
      case maxD ~ max ~ s1 ~ minD ~ min ~ s2 ~ avg =>
      PriceStats(maxD, max, s1, minD, min, s2, avg)
    }
  }

  def game(name: String) = {
    DB.withConnection { implicit connection =>
      SQL("""
        select first_value(date_recorded) over() as d1, 
        first_value(price_on_x) over() as max, first_value(store) over() as s1,
        last_value(date_recorded) over() as d2, 
        last_value(price_on_x) over() as min, last_value(store) over() as s2,
        avg(price_on_x) over() from (
          select price_on_x, date_recorded, store from (
            (select * from scraped_games
              where scraped_games.unq_game_id = (select distinct on (unq_game_id) unq_game_id from 
                scraped_games where lower(name) = lower({name}))) as test1
      left join
      (select * from price_history) as test2
      on test1.id = test2.game_id) as trans
      order by price_on_x desc, date_recorded desc
      ) as trans1
      limit 1
      """).on('name -> name).as(simple.single)

    }
  }

}
