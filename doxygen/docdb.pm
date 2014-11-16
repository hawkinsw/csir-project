#!/usr/bin/perl

use strict;
use DBI;

package docdb;
$docdb::INSERT_PACKAGE_SQL = "INSERT INTO package (name, package_file_name, package_url) VALUES (?,?,?)";
$docdb::INSERT_DOCUMENTATION_SQL = "INSERT INTO documentation (package_id, source_id, documentation) VALUES (?,?,?)";
$docdb::INSERT_SOURCE_SQL = "INSERT INTO source (package_id, type, return_type, name, parameter_count, source) VALUES (?,?,?,?,?,?)";
$docdb::SELECT_PACKAGE_ID_SQL = "SELECT id FROM package WHERE name=?";
$docdb::SELECT_SOURCE_ID_SQL = "SELECT id FROM source WHERE package_id=? and name=? and parameter_count=?";
$docdb::SELECT_GLOBAL_SOURCE_ID_SQL = "SELECT id FROM source WHERE name=?";
$docdb::UPDATE_SOURCE_SQL = "UPDATE source SET source=? WHERE package_id=? and id=?";
$docdb::INSERT_PARAMETER_SQL = "INSERT INTO parameter (package_id, source_id, type, name) VALUES (?,?,?,?)";
$docdb::INSERT_DEPENDENCY_NAME_SQL = "INSERT INTO dependency (package_id, source_id, depends_on_name) VALUES (?,?,?)";
$docdb::INSERT_DEPENDENCY_ID_SQL = "INSERT INTO dependency (package_id, source_id, depends_on_id) VALUES (?,?,?)";
$docdb::UPDATE_DEPENDENCY_ID_SQL = "UPDATE dependency SET depends_on_id=?, depends_on_name=\"\" WHERE depends_on_name=?";

sub new {
	my $class = shift;

	my $self = { mysqlHost => shift,
		mysqlUser => shift,
		mysqlPass => shift,
		mysqlDb => shift,
		is_connected => 0};

	$self->{dsn} = "DBI:mysql:database=$self->{mysqlDb};host=$self->{mysqlHost}";
	bless $self, $class;
	return $self;
}

sub disconnect {
	my $self = shift;
	if (!$self->{is_connected}) { return 1 };

	$self->{dbh}->disconnect();

	return 1;
}

sub connect {
	my $self = shift;
	$self->{dbh} = DBI->connect($self->{dsn},
		$self->{mysqlUser},
		$self->{mysqlPass},
		{RaiseError=>1});

	if ($self->{dbh} != undef) {
		$self->{is_connected} = 1;
	}

	eval {
		$self->{insert_package}
			= $self->{dbh}->prepare($docdb::INSERT_PACKAGE_SQL);
		$self->{insert_documentation}
			= $self->{dbh}->prepare($docdb::INSERT_DOCUMENTATION_SQL);
		$self->{insert_source}
			= $self->{dbh}->prepare($docdb::INSERT_SOURCE_SQL);
		$self->{select_package}
			= $self->{dbh}->prepare($docdb::SELECT_PACKAGE_ID_SQL);
		$self->{select_source_id}
			= $self->{dbh}->prepare($docdb::SELECT_SOURCE_ID_SQL);
		$self->{select_global_source_id}
			= $self->{dbh}->prepare($docdb::SELECT_GLOBAL_SOURCE_ID_SQL);
		$self->{update_source}
			= $self->{dbh}->prepare($docdb::UPDATE_SOURCE_SQL);
		$self->{insert_parameter}
			= $self->{dbh}->prepare($docdb::INSERT_PARAMETER_SQL);
		$self->{insert_dependency_name}
			= $self->{dbh}->prepare($docdb::INSERT_DEPENDENCY_NAME_SQL);
		$self->{insert_dependency_id}
			= $self->{dbh}->prepare($docdb::INSERT_DEPENDENCY_ID_SQL);
		$self->{update_dependency_id}
			= $self->{dbh}->prepare($docdb::UPDATE_DEPENDENCY_ID_SQL);
	};
	if ($@) {
		$self->{dbh}->disconnect();
		$self->{is_connected} = 0;
	}
	return $self->{is_connected};
}

sub add_documentation {
	my $self = shift;
	my $package_id = shift;
	my $source_id = shift;
	my $doc = shift;

	return -1 unless ($self->{is_connected});
	my $documentation_id = eval {
		$self->{insert_documentation}->bind_param(1, $package_id);
		$self->{insert_documentation}->bind_param(2, $source_id);
		$self->{insert_documentation}->bind_param(3, $doc);

		$self->{insert_documentation}->execute();

		return $self->{insert_documentation}->{mysql_insertid};
	};
	if ($@) {
		return -1;
	}
	return $documentation_id;
}


#sub update_source {
#	my $self = shift;
#	my $package_id = shift;
#	my $source_id = shift;
#	my $source = shift;

#	return -1 unless ($self->{is_connected});
#	eval {
#		$self->{update_source}->bind_param(2, $package_id);
#		$self->{update_source}->bind_param(3, $source_id);
#		$self->{update_source}->bind_param(1, $source);

#		$self->{update_source}->execute();
#	};
#	if ($@) {
#		return 0;
#	}
#	return 1;
#}

sub add_parameter {
	my $self = shift;
	my $package_id = shift;
	my $source_id = shift;
	my $type = shift;
	my $name = shift;

	return -1 unless ($self->{is_connected});
	my $parameter_id = eval {
		$self->{insert_parameter}->bind_param(1, $package_id);
		$self->{insert_parameter}->bind_param(2, $source_id);
		$self->{insert_parameter}->bind_param(3, $type);
		$self->{insert_parameter}->bind_param(4, $name);

		$self->{insert_parameter}->execute();

		return $self->{insert_parameter}->{mysql_insertid};
	};
	if ($@) {
		return -1;
	}
	return $parameter_id;
}


sub add_source {
	my $self = shift;
	my $package_id = shift;
	my $type = shift;
	my $return_type = shift;
	my $name = shift;
	my $parameter_count = shift;
	my $code = shift;

	return -1 unless ($self->{is_connected});
	my $source_id = eval {
		$self->{insert_source}->bind_param(1, $package_id);
		$self->{insert_source}->bind_param(2, $type);
		$self->{insert_source}->bind_param(3, $return_type);
		$self->{insert_source}->bind_param(4, $name);
		$self->{insert_source}->bind_param(5, $parameter_count);
		$self->{insert_source}->bind_param(6, $code);

		$self->{insert_source}->execute();

		return $self->{insert_source}->{mysql_insertid};
	};
	if ($@) {
		return -1;
	}
	return $source_id;
}

sub add_package {
	my $self = shift;
	my $name = shift;
	my $filename = shift;
	my $url = shift;

	return -1 unless ($self->{is_connected});
	my $package_id = eval {
		$self->{insert_package}->bind_param(1, $name);
		$self->{insert_package}->bind_param(2, $filename);
		$self->{insert_package}->bind_param(3, $url);

		$self->{insert_package}->execute();

		return $self->{insert_package}->{mysql_insertid};
	};
	if ($@) {
		return -1;
	}
	return $package_id;
}
