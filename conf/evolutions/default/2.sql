# --- Second database schema

# --- !Ups

create index on scraped_games (unq_game_id);

# --- !Downs
