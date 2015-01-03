#!/usr/bin/perl

my $name, $package, $directory, $url, $db_host, $db_user, $db_pass, $db_db;
BEGIN {
$name = shift;
$package = shift;
$url = shift;
$directory = shift;
$db_host = shift;
$db_user = shift;
$db_pass = shift;
$db_db = shift;
}

$directory = quotemeta quotemeta $directory;

system("sed s/__ID__/$directory/ doxygen/doxygen.cfg | sed s/__OD__/$directory/ | /usr/bin/doxygen -");
