package models

import java.util.Date

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

import models.Game._
import models.Price._
import models.SteamGame._

case class GwithP(g: Game, p: Price)

case class GSwP(sg: SteamGame, gwp: GwithP)

case class MatchedIds(selectedTerm: List[String])


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

case class DataCleanup(sim: Double, n1: String, id1: Int, url1: String,
  n2: String, id2: Int, url2: String)

object DataCleanup {

  val simple = {
    get[Double]("similarity") ~
      get[String]("scraped_games.n1.name") ~
      get[Int]("scraped_games.n1.unq_game_id") ~
      get[String]("scraped_games.n1.store_url") ~
      get[String]("scraped_games.n2.name") ~
      get[Int]("scraped_games.n2.unq_game_id") ~
      get[String]("scraped_games.n2.store_url") map {
        case sim ~ n1 ~ id1 ~ url1 ~ n2 ~ id2 ~ url2 =>
          DataCleanup(sim, n1, id1, url1, n2, id2, url2)
      }
  }

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

  def matchManually(page: Int = 0, pageSize: Int = 25, filter: String = "%") = {

    val offset = pageSize * page

    val matchedPairs = DB.withConnection { implicit connection =>
      SQL(
        """
      select *, count(*) over() as full_count from (
          SELECT distinct on(n1.unq_game_id,n2.unq_game_id) cast(similarity(n1.name, n2.name) as double precision), 
          n1.name as "n1.name", n1.unq_game_id as "n1.unq_game_id", n1.store_url as "n1.store_url",
          n2.name as "n2.name", n2.unq_game_id as "n2.unq_game_id", n2.store_url as "n2.store_url"
          FROM (select * from scraped_games where name ilike {filter}) n1, 
          (select * from scraped_games where name ilike {filter}) n2 where
           lower(substring(n1.name from 1 for 3)) = lower(substring(n2.name from 1 for 3)) 
           AND n1.id < n2.id and n1.unq_game_id < n2.unq_game_id
           and n1.store <> n2.store
           and levenshtein(n1.name,n2.name)<15) as q1
      order by similarity desc, "n1.unq_game_id"
      limit {pagesize} offset {offset}
      """).on('pagesize -> pageSize, 'offset -> offset, 'filter -> ("%" + filter + "%"))
        .as(DataCleanup.simple ~ get[Long]("full_count") *) map {
          case a ~ b => (a, b)
        }
    }
    if (!matchedPairs.isEmpty) {
    Page(matchedPairs.map(_._1), page, offset, matchedPairs.map(_._2).head)
    } else Page(Seq(DataCleanup(0, "", 0, "", "", 0, "")), 0, 0, 0)

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
      order by meta_critic desc nulls last
      limit 20
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
  avg: Double) {

  def shortStoreName(m: Map[String, String]): PriceStats = {
    PriceStats(maxD, max, m.getOrElse(smax, ""), minD, min,
      m.getOrElse(smin, ""), avg)
  }
}

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

  def game(name: String): Option[PriceStats] = {
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
      """).on('name -> name).as(simple.singleOpt)

    }
  }

}

case class SearchResult(g: GwithP, sim: Double, cnt: Long)

object SearchResult {

  val simple = {
    get[Double]("similarity") ~
      get[Long]("c1") map {
        case a ~ b => (a, b)
      }
  }

  val withCount = Game.withPrice ~ simple map {
    case a ~ b => SearchResult(a, b._1, b._2)
  }

  def singleOrList(name: String): List[SearchResult] = {
    val single = DB.withConnection { implicit conection =>
      SQL("""
        select *, cast(similarity(name,{name}) as double precision), count(unq_game_id) over() as c1 from (
        select distinct on(unq_game_id) * from (
        select * from scraped_games 
        left join price_history on game_id = scraped_games.id
        where name = {name}) as q1) as q2 
      """).on('name -> name).as(withCount *)
    }

    val list = DB.withConnection { implicit connection =>
      SQL("""
          select *, cast(similarity(name,{name}) as double precision), count(unq_game_id) over() as c1 from (
          select distinct on(unq_game_id) * from (
          select * from scraped_games
          left join price_history on game_id = scraped_games.id
          where unq_game_id in (select unq_game_id from scraped_games where name % {name})) as s1
          order by unq_game_id, date_recorded desc) as s2
          order by similarity desc
      """).on('name -> name).as(withCount *)
    }

    if (!single.isEmpty) single ::: list.tail
    else list
  }

}

case class StoreComparison(store: String, dateRecorded: Date, count: Long)

object StoreComparison {

  val simple = {
    get[String]("store") ~
      get[Date]("date_recorded") ~
      get[Long]("count") map {
        case store ~ dateRecorded ~ count =>
          StoreComparison(store, dateRecorded, count)
      }
  }

  def timeLine = {
    DB.withConnection { implicit connection =>
      SQL("""
    		with jjj as (
    		select id,name,store,unq_game_id,price_on_x,date_recorded from (
    		select * from scraped_games as g1
    		join (select price_on_x, date_recorded, game_id from price_history) p1 on p1.game_id = g1.id
    		where unq_game_id  in (
    		select unq_game_id from (
    		select unq_game_id, count(unq_game_id) as t1 from scraped_games
    		group by unq_game_id) as q1
    		where t1 > 3)) as foo)
    		select store,date_recorded,count(store) from (
    		select * from jjj where
    		(unq_game_id, date_recorded, price_on_x) in (select unq_game_id,date_recorded,min(price_on_x) as m1 from jjj
    		group by unq_game_id,date_recorded)) as q5
    		group by store,date_recorded
        """).as(simple *)
    }
  }

}
