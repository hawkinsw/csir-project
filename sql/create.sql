use ir;

drop table if exists package;
drop table if exists source;
drop table if exists documentation;

create table package (
	id integer not null primary key auto_increment,
	name varchar(255),
	package_file_name text,
	package_url text);

create table source (
	id integer not null primary key auto_increment,
	package_id integer not null,
	type enum('class', 'method') not null default 'method',
	name text not null,
	source text);

create table documentation (
	id integer not null primary key auto_increment,
	package_id integer not null,
	source_id integer not null,
	documentation text);

