#!/usr/bin/perl

use lib "./";
use docdb;

$db = new docdb("localhost", "ir", "ir", "ir");

if (!$db->connect()) {
	die "Not connected!\n";
}
print "Connected!\n";

$new_package_id = $db->add_package("test-perl1", "test-perl2", "test-perl3");
$new_source_id = $db->add_source($new_package_id, "method", "return", "name", 500, "code");
$new_parameter_id = $db->add_parameter($new_package_id, $new_source_id, "type", "name");
#$db->update_source($new_package_id, $new_source_id, "code2");

print "new_package_id: $new_package_id \n";
print "new_source_id: $new_source_id \n";
print "new_parameter_id: $new_parameter_id \n";

$db->disconnect();
