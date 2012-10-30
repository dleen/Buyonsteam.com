# --- First database schema

# --- !Ups

create table games (
	id				serial not null primary key,
	steam_id		int unique check (steam_id > 0),
	name			varchar(255) not null unique,
	url				varchar(255) not null,
	img_url			varchar(255) not null,
	release_date	varchar(255) not null,
	meta_critic		int check (meta_critic > 0)
);

create table gamersgate_leftover (
	id				serial not null primary key,
	name			varchar(255) not null unique
);

create table price_history (
	id				serial not null primary key,
	name			varchar(255) not null,
	price_on_steam	double precision check (price_on_steam > 0),
	price_on_amazon	double precision check (price_on_amazon > 0),
	on_sale			boolean not null,
	date_recorded	date not null,
	game_id			int not null references games (id),
	unique (date_recorded, game_id)
);

create index on games (name);

CREATE INDEX game_name_trigram_idx ON games USING gist(name gist_trgm_ops);

create index on price_history (name);

# --- !Downs

drop table price_history;

drop table games;

