#include "docdb.hpp"
#include <iostream>
#include <string>

using namespace std;
using namespace mysqlpp;

#define CHECK_CONNECTED(a) \
if (!a) return -1;

void printException(const Exception &ex) {
	cout << "Exception: " << ex.what() << endl;
}

DocDb::DocDb(string mysqlHost,
	string mysqlUser,
	string mysqlPass,
	string mysqlDa) {

	m_mysqlUser = mysqlUser;
	m_mysqlPass = mysqlPass;
	m_mysqlHost = mysqlHost;
	m_mysqlDb = mysqlDa;

	m_isConnected = false;
}

bool DocDb::connect() {
	m_con = new Connection();

	if (!m_con->connect(m_mysqlDb.c_str(),
	    m_mysqlHost.c_str(),
			m_mysqlUser.c_str(),
			m_mysqlPass.c_str())) {
		return false;
	}

	if (!prepareQueries()) {
		return false;
	}
	m_isConnected = true;
	return true;
}

int DocDb::addPackage(string name, string filename, string url) {
	CHECK_CONNECTED(m_isConnected);
	try {
		int id = -1;
		SimpleResult res;

		res = m_addPackageQuery->execute(name, filename, url);
		id = res.insert_id();

		return id;
	} catch (const Exception &ex) {
		printException(ex);
		return -1;
	}
}

int DocDb::addSource(int packageId, string type, string name, string code) {
	CHECK_CONNECTED(m_isConnected);
	try {
		int id = -1;
		SimpleResult res;

		res = m_addSourceQuery->execute(packageId, type, name, code);
		id = res.insert_id();

		return id;
	} catch (const Exception &ex) {
		printException(ex);
		return -1;
	}
}

int DocDb::addDocumentation(int packageId, int sourceId, string documentation) {
	CHECK_CONNECTED(m_isConnected);
	try {
		int id = -1;
		SimpleResult res;

		res = m_addDocumentationQuery->execute(packageId, sourceId, documentation);
		id = res.insert_id();

		return id;
	} catch (const Exception &ex) {
		printException(ex);
		return -1;
	}
}

int DocDb::getPackageIdFromName(string name) {
	CHECK_CONNECTED(m_isConnected);
	try {
		string idAsString;
		StoreQueryResult res;

		res = m_getPackageIdQuery->store(name);
		if (res.empty())
			return -1;
		
		idAsString = string(res[0][0]);
		return std::stoi(idAsString);
	} catch (const Exception &ex) {
		printException(ex);
		return -1;
	}
}

int DocDb::getSourceIdFromName(int packageId, string name) {
	CHECK_CONNECTED(m_isConnected);
	try {
		string idAsString;
		StoreQueryResult res;

		res = m_getSourceIdQuery->store(packageId, name);
		if (res.empty())
			return -1;
		
		idAsString = string(res[0][0]);
		return std::stoi(idAsString);
	} catch (const Exception &ex) {
		printException(ex);
		return -1;
	}
}

bool DocDb::prepareQueries() {
	try {
		m_addPackageQuery = new Query(m_con);
		(*m_addPackageQuery) << "INSERT INTO package "
			"(name, package_file_name, package_url) "
			"VALUES "
			"(%0q,%1q,%2q)";
		m_addPackageQuery->parse();

		m_addSourceQuery = new Query(m_con);
		(*m_addSourceQuery) << "INSERT INTO source "
			"(package_id, type, name, source) "
			"VALUES "
			"(%0q,%1q,%2q, %3q)";
		m_addSourceQuery->parse();

		m_addDocumentationQuery = new Query(m_con);
		(*m_addDocumentationQuery) << "INSERT INTO documentation "
			"(package_id, source_id, documentation) "
			"VALUES "
			"(%0q,%1q,%2q)";
		m_addDocumentationQuery->parse();

		m_getPackageIdQuery = new Query(m_con);
		(*m_getPackageIdQuery) << "SELECT id "
			"FROM package "
			"WHERE "
			"name=%0q";
		m_getPackageIdQuery->parse();

		m_getSourceIdQuery = new Query(m_con);
		(*m_getSourceIdQuery) << "SELECT id "
			"FROM source "
			"WHERE "
			"package_id=%0q and name=%1q";
		m_getSourceIdQuery->parse();

	} catch (const Exception &ex) {
		printException(ex);
		return false;
	}
	return true;
}

bool DocDb::disconnect() {
	if (m_isConnected)
		m_con->disconnect();

	m_isConnected = false;
	return true;
}
