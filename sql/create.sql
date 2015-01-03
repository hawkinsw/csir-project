use ir;

drop table if exists package;
drop table if exists source;
drop table if exists documentation;
drop table if exists parameter;
drop table if exists dependency;
drop table if exists synonyms;
drop table if exists parents;

create table package (
	id integer not null primary key auto_increment,
	name varchar(255),
	package_source_language varchar(255),
	package_file_name text,
	package_url text);

create table source (
	id integer not null primary key auto_increment,
	package_id integer not null,
	type enum('class', 'method', 'namespace') not null default 'method',
	name text not null,
	return_type varchar(255),
	parameter_count integer not null default 1,
	source text);

create table parents (
	package_id integer not null,
	source_id integer not null,
	parent_id integer,
	parent_name varchar(255),
	primary key (package_id, source_id, parent_id, parent_name));

create table parameter (
	id integer not null primary key auto_increment,
	package_id integer not null,
	source_id integer not null,
	type varchar(255) not null,
	name varchar(255) not null);

create table documentation (
	id integer not null primary key auto_increment,
	package_id integer not null,
	source_id integer not null,
	documentation text);

create table dependency (
	id integer not null primary key auto_increment,
	package_id integer not null,
	source_id integer not null,
	depends_on_id integer,
	depends_on_name varchar(255));

create table synonyms (
	id integer not null primary key auto_increment,
	package_id integer not null,
	count integer not null default 0,
	word1 varchar(255),
	word2 varchar(255));
