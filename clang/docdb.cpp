#include "docdb.hpp"
#include <iostream>
#include <string>
#include <mysql++/mysql++.h>

using namespace std;
using namespace mysqlpp;

#define CHECK_CONNECTED(a) \
if (!a) return -1;

/*
 * Required to keep mysql headers from
 * polluting the docdb.hpp file. If those
 * files are included, then clang cannot
 * compile since they require RTTI and clang
 * (at least in this configuration) refuses
 * to compile with that option.
 */
class DocDbMysqlWrapper {
	public:
		bool isConnected;
		mysqlpp::Connection *con;
		mysqlpp::Query *addPackageQuery,
			*addSourceQuery,
			*addDocumentationQuery,
			*getPackageIdQuery,
			*getSourceIdQuery,
			*updateSourceQuery;
		std::string mysqlUser;
		std::string mysqlPass;
		std::string mysqlHost;
		std::string mysqlDb;
};

void printException(const Exception &ex) {
	cout << "Exception: " << ex.what() << endl;
}

DocDb::DocDb(string mysqlHost,
	string mysqlUser,
	string mysqlPass,
	string mysqlDa) {

	m_wrapper = new DocDbMysqlWrapper();

	m_wrapper->mysqlUser = mysqlUser;
	m_wrapper->mysqlPass = mysqlPass;
	m_wrapper->mysqlHost = mysqlHost;
	m_wrapper->mysqlDb = mysqlDa;

	m_wrapper->isConnected = false;
}

bool DocDb::connect() {
	m_wrapper->con = new Connection();

	if (!m_wrapper->con->connect(m_wrapper->mysqlDb.c_str(),
	    m_wrapper->mysqlHost.c_str(),
			m_wrapper->mysqlUser.c_str(),
			m_wrapper->mysqlPass.c_str())) {
		return false;
	}

	if (!prepareQueries()) {
		return false;
	}
	m_wrapper->isConnected = true;
	return true;
}

int DocDb::addPackage(string name, string filename, string url) {
	CHECK_CONNECTED(m_wrapper->isConnected);
	try {
		int id = -1;
		SimpleResult res;

		res = m_wrapper->addPackageQuery->execute(name, filename, url);
		id = res.insert_id();

		return id;
	} catch (const Exception &ex) {
		printException(ex);
		return -1;
	}
}

int DocDb::addSource(int packageId, string type, string name, string code) {
	CHECK_CONNECTED(m_wrapper->isConnected);
	try {
		int id = -1;
		SimpleResult res;

		res = m_wrapper->addSourceQuery->execute(packageId, type, name, code);
		id = res.insert_id();

		return id;
	} catch (const Exception &ex) {
		printException(ex);
		return -1;
	}
}

int DocDb::addDocumentation(int packageId, int sourceId, string documentation) {
	CHECK_CONNECTED(m_wrapper->isConnected);
	try {
		int id = -1;
		SimpleResult res;

		res = m_wrapper->addDocumentationQuery->execute(packageId, sourceId, documentation);
		id = res.insert_id();

		return id;
	} catch (const Exception &ex) {
		printException(ex);
		return -1;
	}
}

int DocDb::getPackageIdFromName(string name) {
	CHECK_CONNECTED(m_wrapper->isConnected);
	try {
		string idAsString;
		StoreQueryResult res;

		res = m_wrapper->getPackageIdQuery->store(name);
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
	CHECK_CONNECTED(m_wrapper->isConnected);
	try {
		string idAsString;
		StoreQueryResult res;

		res = m_wrapper->getSourceIdQuery->store(packageId, name);
		if (res.empty())
			return -1;
		
		idAsString = string(res[0][0]);
		return std::stoi(idAsString);
	} catch (const Exception &ex) {
		printException(ex);
		return -1;
	}
}

bool DocDb::updateSource(int packageId, int sourceId, string source) {
	CHECK_CONNECTED(m_wrapper->isConnected);
	try {
		SimpleResult res;

		res = m_wrapper->updateSourceQuery->execute(packageId, sourceId, source);
		if (!res)
			return false;

		return true;
	} catch (const Exception &ex) {
		printException(ex);
		return false;
	}
}

bool DocDb::prepareQueries() {
	try {
		m_wrapper->addPackageQuery = new Query(m_wrapper->con);
		(*m_wrapper->addPackageQuery) << "INSERT INTO package "
			"(name, package_file_name, package_url) "
			"VALUES "
			"(%0q,%1q,%2q)";
		m_wrapper->addPackageQuery->parse();

		m_wrapper->addSourceQuery = new Query(m_wrapper->con);
		(*m_wrapper->addSourceQuery) << "INSERT INTO source "
			"(package_id, type, name, source) "
			"VALUES "
			"(%0q,%1q,%2q, %3q)";
		m_wrapper->addSourceQuery->parse();

		m_wrapper->addDocumentationQuery = new Query(m_wrapper->con);
		(*m_wrapper->addDocumentationQuery) << "INSERT INTO documentation "
			"(package_id, source_id, documentation) "
			"VALUES "
			"(%0q,%1q,%2q)";
		m_wrapper->addDocumentationQuery->parse();

		m_wrapper->getPackageIdQuery = new Query(m_wrapper->con);
		(*m_wrapper->getPackageIdQuery) << "SELECT id "
			"FROM package "
			"WHERE "
			"name=%0q";
		m_wrapper->getPackageIdQuery->parse();

		m_wrapper->getSourceIdQuery = new Query(m_wrapper->con);
		(*m_wrapper->getSourceIdQuery) << "SELECT id "
			"FROM source "
			"WHERE "
			"package_id=%0q and name=%1q";
		m_wrapper->getSourceIdQuery->parse();

		m_wrapper->updateSourceQuery = new Query(m_wrapper->con);
		(*m_wrapper->updateSourceQuery) << "UPDATE source SET "
			"source=%2q "
			"WHERE "
			"package_id=%0q and name=%1q";
		m_wrapper->updateSourceQuery->parse();

	} catch (const Exception &ex) {
		printException(ex);
		return false;
	}
	return true;
}

bool DocDb::disconnect() {
	if (m_wrapper->isConnected)
		m_wrapper->con->disconnect();

	m_wrapper->isConnected = false;
	return true;
}
