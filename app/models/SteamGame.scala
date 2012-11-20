package models

import java.util.Date

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class SteamGame(steamId: Int,
	name: String,
	releaseDate: String,
	metacritic: Int,
	gameId: Int)


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