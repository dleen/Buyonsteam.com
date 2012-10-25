# --- First database schema

# --- !Ups

create table games (
	id				serial not null primary key,
	steam_id		int,
	game_name		varchar(255) not null,
	url				varchar(255) not null,
	img_url			varchar(255) not null,
	release_date	varchar(255) not null,
	meta_critic		int
);

create table price_history (
	id				serial not null primary key,
	game_name		varchar(255) not null,
	price_on_steam	numeric,
	price_on_amazon	numeric,
	date_recorded	date not null,
	game_id			bigint not null references games (id) 
);

create index on games (game_name);

create index on price_history (game_name);

# --- !Downs

drop table price_history;

drop table games;

