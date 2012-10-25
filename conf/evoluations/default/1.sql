# --- First database schema

# --- !Ups

create table games (
	id				bigint not null,
	steam_id		int,
	game_name		varchar(255) not null,
	url				varchar(255) not null,
	img_url			varchar(255),
	release_date	varchar(255),
	meta_critic		int
);

create table price_history (
	id				bigint not null,
	price			real
);




# --- !Downs

drop table price_history

drop table games

