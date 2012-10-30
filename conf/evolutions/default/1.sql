# --- First database schema

# --- !Ups

create extension if not exists pg_trgm;

create table scraped_games (
	id				serial not null primary key,
	name			varchar(255) not null,
	store			varchar(255) not null,
	store_url		varchar(255) not null,
	img_url			varchar(255) not null,
	unique (name, store)
);

create table steam_games (
	steam_id		int primary key,
	name			varchar(255) not null unique,
	release_date	varchar(255) not null,
	meta_critic		int not null,
	game_id			int not null references scraped_games (id)
);

create table price_history (
	id				serial not null primary key,
	price_on_x		double precision not null check (price_on_x >= 0),
	on_sale			boolean not null,
	date_recorded	date not null,
	game_id			int not null references scraped_games (id),
	unique (date_recorded, game_id)
);

create index on scraped_games (name);

create index on steam_games (name);

create index game_name_trigram_idx on scraped_games using gist(name gist_trgm_ops);

create index on price_history (game_id);

# --- !Downs

drop table price_history;

drop table steam_games;

drop table scraped_games;
