package edu.virginia.cs;

import java.lang.System;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.CallableStatement;
import java.sql.ResultSet;

public class DocDb {
	private String mMysqlHost = null, 
		mMysqlUser = null,
		mMysqlPass = null,
		mMysqlDb = null;
	private boolean mIsConnected = false;
	private Connection mSqlConnection = null;
	private PreparedStatement mInsertPackageStmt = null;
	private PreparedStatement mInsertDocumentationStmt = null;
	private PreparedStatement mGetDocumentationStmt = null;
	private PreparedStatement mInsertSourceStmt = null;
	private PreparedStatement mGetPackageIdStmt = null;
	private PreparedStatement mGetSourceIdStmt = null;
	private PreparedStatement mGetSourceStmt = null;
	private PreparedStatement mGetGlobalSourceIdStmt = null;
	private PreparedStatement mUpdateSourceStmt = null;
	private PreparedStatement mInsertParameterStmt = null;
	private PreparedStatement mInsertParentNameStmt = null;
	private PreparedStatement mInsertParentIdStmt = null;
	private PreparedStatement mInsertDependencyNameStmt = null;
	private PreparedStatement mInsertDependencyIdStmt = null;
	private PreparedStatement mUpdateDependencyIdStmt = null;
	private PreparedStatement mUpdateParentIdStmt = null;
	private CallableStatement mGetParentsStmt = null;
	private PreparedStatement mGetMemberIdStmt = null;
	private PreparedStatement mGetSourceIdsStmt = null;
	private PreparedStatement mCountIdsStmt = null;
	private PreparedStatement mGetNameStmt = null;
	private PreparedStatement mGetInvocationsStmt = null;
	private PreparedStatement mGetVariablesStmt = null;
	private PreparedStatement mGetSourceIdsOfMethodsStmt = null;
	private PreparedStatement mGetSourceIdsOfClassesStmt = null;
	private PreparedStatement mCountIdsOfMethodsStmt = null;
	private PreparedStatement mCountIdsOfClassesStmt = null;

	private static final String CALL_PARENTS_LIST_SQL = "{call parent_list(?)}";
	private static final String INSERT_PACKAGE_SQL = "INSERT INTO package (name, package_file_name, package_url, package_source_language) VALUES (?,?,?, \"Java\")";
	private static final String INSERT_DOCUMENTATION_SQL = "INSERT INTO documentation (package_id, source_id, documentation) VALUES (?,?,?)";
	private static final String SELECT_DOCUMENTATION_SQL = "SELECT documentation from documentation where source_id=?";
	private static final String INSERT_SOURCE_SQL = "INSERT INTO source (package_id, type, member_id, return_type, name, parameter_count, source) VALUES (?,?,?,?,?,?,?)";
	private static final String INSERT_PARENT_NAME_SQL = "INSERT INTO parents (package_id, source_id, parent_name) VALUES (?,?,?)";
	private static final String INSERT_PARENT_ID_SQL = "INSERT INTO parents (package_id, source_id, parent_id) VALUES (?,?,?)";
	private static final String UPDATE_PARENT_ID_SQL = "UPDATE parents SET parent_id=?, parent_name=\"\" WHERE parent_name=?";
	private static final String SELECT_PACKAGE_ID_SQL = "SELECT id FROM package WHERE name=?";
	private static final String SELECT_SOURCE_ID_SQL = "SELECT id FROM source WHERE package_id=? and name=? and parameter_count=?";
	private static final String SELECT_SOURCE_SQL = "SELECT source FROM source WHERE id=?";
	private static final String SELECT_GLOBAL_SOURCE_ID_SQL = "SELECT id FROM source WHERE name=?";
	private static final String UPDATE_SOURCE_SQL = "UPDATE source SET source=? WHERE package_id=? and id=?";
	private static final String INSERT_PARAMETER_SQL = "INSERT INTO parameter (package_id, source_id, type, name) VALUES (?,?,?,?)";
	private static final String INSERT_DEPENDENCY_NAME_SQL = "INSERT INTO dependency (package_id, source_id, depends_on_name) VALUES (?,?,?)";
	private static final String INSERT_DEPENDENCY_ID_SQL = "INSERT INTO dependency (package_id, source_id, depends_on_id) VALUES (?,?,?)";
	private static final String UPDATE_DEPENDENCY_ID_SQL = "UPDATE dependency SET depends_on_id=?, depends_on_name=\"\" WHERE depends_on_name=?";
	private static final String SELECT_MEMBER_ID_SQL = "SELECT member_id FROM source WHERE id=?";
	private static final String SELECT_SOURCE_IDS_SQL = "SELECT id FROM source"; 
	private static final String SELECT_SOURCE_IDS_OF_METHODS_SQL = "SELECT id FROM source where type=\"method\""; 
	private static final String SELECT_SOURCE_IDS_OF_CLASSES_SQL = "SELECT id FROM source where type=\"class\""; 
	private static final String SELECT_IDS_COUNT_SQL = "SELECT count(id) FROM source"; 
	private static final String SELECT_IDS_OF_METHODS_COUNT_SQL = "SELECT count(id) FROM source where type=\"method\""; 
	private static final String SELECT_IDS_OF_CLASSES_COUNT_SQL = "SELECT count(id) FROM source where type=\"class\""; 
	private static final String SELECT_NAME_SQL = "SELECT name FROM source where id=?"; 
	private static final String SELECT_INVOCATIONS_SQL = "SELECT invocations FROM source where id=?";
	private static final String SELECT_VARIABLES_SQL = "SELECT variables FROM source where id=?";

	public DocDb(String mysqlHost,
		String mysqlUser,
		String mysqlPass,
		String mysqlDb) {
		mMysqlHost = mysqlHost;
		mMysqlUser = mysqlUser;
		mMysqlPass = mysqlPass;
		mMysqlDb = mysqlDb;
	}

	public boolean connect() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			mSqlConnection = DriverManager.getConnection("jdbc:mysql://" +
				mMysqlHost + 
				"/" + mMysqlDb + 
				"?user=" + mMysqlUser + 
				"&password=" + mMysqlPass);
			mIsConnected = true;
		} catch (Exception e) {
			System.out.println("Could not connect to MySql database: "+e.toString());
			mSqlConnection = null;
		}
		if (mIsConnected) 
			prepareStatements();

		return mIsConnected;
	}

	private void prepareStatements() {
		try {
			mInsertPackageStmt = mSqlConnection.prepareStatement(
				INSERT_PACKAGE_SQL, 
				Statement.RETURN_GENERATED_KEYS);
			mInsertDocumentationStmt = mSqlConnection.prepareStatement(
				INSERT_DOCUMENTATION_SQL,
				Statement.RETURN_GENERATED_KEYS);
			mInsertSourceStmt = mSqlConnection.prepareStatement(
				INSERT_SOURCE_SQL,
				Statement.RETURN_GENERATED_KEYS);
			mGetPackageIdStmt = mSqlConnection.prepareStatement(
				SELECT_PACKAGE_ID_SQL);
			mGetSourceIdStmt = mSqlConnection.prepareStatement(
				SELECT_SOURCE_ID_SQL);
			mGetSourceStmt = mSqlConnection.prepareStatement(
				SELECT_SOURCE_SQL);
			mGetGlobalSourceIdStmt = mSqlConnection.prepareStatement(
				SELECT_GLOBAL_SOURCE_ID_SQL);
			mUpdateSourceStmt = mSqlConnection.prepareStatement(
				UPDATE_SOURCE_SQL);
			mInsertParameterStmt = mSqlConnection.prepareStatement(
				INSERT_PARAMETER_SQL,
				Statement.RETURN_GENERATED_KEYS);
			mInsertDependencyNameStmt = mSqlConnection.prepareStatement(
				INSERT_DEPENDENCY_NAME_SQL,
				Statement.RETURN_GENERATED_KEYS);
			mInsertDependencyIdStmt = mSqlConnection.prepareStatement(
				INSERT_DEPENDENCY_ID_SQL,
				Statement.RETURN_GENERATED_KEYS);
			mUpdateDependencyIdStmt = mSqlConnection.prepareStatement(
				UPDATE_DEPENDENCY_ID_SQL);
			mInsertParentNameStmt = mSqlConnection.prepareStatement(
				INSERT_PARENT_NAME_SQL);
			mInsertParentIdStmt = mSqlConnection.prepareStatement(
				INSERT_PARENT_ID_SQL);
			mUpdateParentIdStmt = mSqlConnection.prepareStatement(
				UPDATE_PARENT_ID_SQL);
			mGetParentsStmt = mSqlConnection.prepareCall(
				CALL_PARENTS_LIST_SQL);
			mGetDocumentationStmt = mSqlConnection.prepareStatement(
				SELECT_DOCUMENTATION_SQL);
			mGetMemberIdStmt = mSqlConnection.prepareStatement(
				SELECT_MEMBER_ID_SQL);
			mGetSourceIdsStmt = mSqlConnection.prepareStatement(
				SELECT_SOURCE_IDS_SQL);
			mGetSourceIdsOfMethodsStmt = mSqlConnection.prepareStatement(
				SELECT_SOURCE_IDS_OF_METHODS_SQL);
			mGetSourceIdsOfClassesStmt = mSqlConnection.prepareStatement(
				SELECT_SOURCE_IDS_OF_CLASSES_SQL);
			mCountIdsStmt = mSqlConnection.prepareStatement(
				SELECT_IDS_COUNT_SQL);
			mCountIdsOfMethodsStmt = mSqlConnection.prepareStatement(
				SELECT_IDS_OF_METHODS_COUNT_SQL);
			mCountIdsOfClassesStmt = mSqlConnection.prepareStatement(
				SELECT_IDS_OF_CLASSES_COUNT_SQL);
			mGetNameStmt  = mSqlConnection.prepareStatement(
				SELECT_NAME_SQL);
			mGetInvocationsStmt = mSqlConnection.prepareStatement(
				SELECT_INVOCATIONS_SQL);
			mGetVariablesStmt = mSqlConnection.prepareStatement(
				SELECT_VARIABLES_SQL);

		} catch (SQLException e) {
			mIsConnected = false;
		}
	}

	private int getAutoId(Statement stmt) {
		int id = -1;
		ResultSet rs = null;
		try {
			rs = stmt.getGeneratedKeys();

			if (rs.next()) {
				id = rs.getInt(1);
			}

			if (rs != null) {
				rs.close();
			}
		} catch (SQLException e) {
			System.err.println("SQL Warning: Could not fetch auto id: " + 
				e.toString());
		}
		return id;
	}

	private int countSourceIdsOfClasses() {
		if (!mIsConnected) return 0;
		try {
			ResultSet idCountRs = null;
			int idCount = 0;

			mCountIdsOfClassesStmt.clearParameters();
			mCountIdsOfClassesStmt.execute();

			idCountRs  = mCountIdsOfClassesStmt.getResultSet();
			if (idCountRs.next()) {
				idCount = idCountRs.getInt(1);
			}

			return idCount;
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return 0;
		}
	}

	private int countSourceIdsOfMethods() {
		if (!mIsConnected) return 0;
		try {
			ResultSet idCountRs = null;
			int idCount = 0;

			mCountIdsOfMethodsStmt.clearParameters();
			mCountIdsOfMethodsStmt.execute();

			idCountRs  = mCountIdsOfMethodsStmt.getResultSet();
			if (idCountRs.next()) {
				idCount = idCountRs.getInt(1);
			}

			return idCount;
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return 0;
		}
	}

	private int countSourceIds() {
		if (!mIsConnected) return 0;
		try {
			ResultSet idCountRs = null;
			int idCount = 0;

			mCountIdsStmt.clearParameters();
			mCountIdsStmt.execute();

			idCountRs  = mCountIdsStmt.getResultSet();
			if (idCountRs.next()) {
				idCount = idCountRs.getInt(1);
			}

			return idCount;
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return 0;
		}
	}

	public String getVariablesFromSourceId(int sourceId) {
		if (!mIsConnected) return null;
		try {
			ResultSet variablesRs = null;
			String variables = "";

			mGetVariablesStmt.clearParameters();
			mGetVariablesStmt.setInt(1, sourceId);
			mGetVariablesStmt.execute();

			variablesRs  = mGetVariablesStmt.getResultSet();
			if (variablesRs.next()) {
				variables = variablesRs.getString(1);
			}

			return variables;
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return null;
		}
	}

	public String getInvocationsFromSourceId(int sourceId) {
		if (!mIsConnected) return null;
		try {
			ResultSet invocationsRs = null;
			String invocations = "";

			mGetInvocationsStmt.clearParameters();
			mGetInvocationsStmt.setInt(1, sourceId);
			mGetInvocationsStmt.execute();

			invocationsRs  = mGetInvocationsStmt.getResultSet();
			if (invocationsRs.next()) {
				invocations = invocationsRs.getString(1);
			}

			return invocations;
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return null;
		}
	}

	public int[] getSourceIdsOfClasses() {
		if (!mIsConnected) return new int[0];
		try {
			int totalSourceIds = 0;
			int i = 0;
			int sourceIds[];
			ResultSet sourceIdRs = null;

			totalSourceIds = countSourceIdsOfClasses();
			sourceIds = new int[totalSourceIds];

			mGetSourceIdsOfClassesStmt.clearParameters();

			mGetSourceIdsOfClassesStmt.execute();

			sourceIdRs  = mGetSourceIdsOfClassesStmt.getResultSet();
			while (sourceIdRs.next() && i < totalSourceIds) {
				sourceIds[i++] = sourceIdRs.getInt(1);
			}

			return sourceIds;
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return new int[0];
		}
	}


	public int[] getSourceIdsOfMethods() {
		if (!mIsConnected) return new int[0];
		try {
			int totalSourceIds = 0;
			int i = 0;
			int sourceIds[];
			ResultSet sourceIdRs = null;

			totalSourceIds = countSourceIdsOfMethods();
			sourceIds = new int[totalSourceIds];

			mGetSourceIdsOfMethodsStmt.clearParameters();

			mGetSourceIdsOfMethodsStmt.execute();

			sourceIdRs  = mGetSourceIdsOfMethodsStmt.getResultSet();
			while (sourceIdRs.next() && i < totalSourceIds) {
				sourceIds[i++] = sourceIdRs.getInt(1);
			}

			return sourceIds;
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return new int[0];
		}
	}

	public int[] getSourceIds() {
		if (!mIsConnected) return new int[0];
		try {
			int totalSourceIds = 0;
			int i = 0;
			int sourceIds[];
			ResultSet sourceIdRs = null;

			totalSourceIds = countSourceIds();
			sourceIds = new int[totalSourceIds];

			mGetSourceIdsStmt.clearParameters();

			mGetSourceIdsStmt.execute();

			sourceIdRs  = mGetSourceIdsStmt.getResultSet();
			while (sourceIdRs.next() && i < totalSourceIds) {
				sourceIds[i++] = sourceIdRs.getInt(1);
			}

			return sourceIds;
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return new int[0];
		}
	}

	public String getNameFromSourceId(int sourceId) {
		if (!mIsConnected) return null;
		try {
			ResultSet nameRs = null;
			String name = null;

			mGetNameStmt.clearParameters();
			mGetNameStmt.setInt(1, sourceId);

			mGetNameStmt.execute();

			nameRs = mGetNameStmt.getResultSet();
			if (nameRs.next()) {
				name = nameRs.getString(1);
			}

			if (nameRs != null) {
				nameRs.close();
			}

			return name;
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return null;
		}
	}

	public String getDocumentationFromSourceId(int sourceId) {
		if (!mIsConnected) return null;
		try {
			ResultSet documentationRs = null;
			String documentation = null;

			mGetDocumentationStmt.clearParameters();
			mGetDocumentationStmt.setInt(1, sourceId);

			mGetDocumentationStmt.execute();

			documentationRs = mGetDocumentationStmt.getResultSet();
			if (documentationRs.next()) {
				documentation = documentationRs.getString(1);
			}

			if (documentationRs != null) {
				documentationRs.close();
			}

			return documentation;
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return null;
		}
	}
	public int[] getParentsFromId(int sourceId) {
		if (!mIsConnected) return new int[0];
		try {
			ResultSet listRs = null;
			String parentsList = null;
			String parsedParentsList[];
			int parents[];
			int counter = 0;

			mGetParentsStmt.clearParameters();
			mGetParentsStmt.setInt(1, sourceId);

			mGetParentsStmt.execute();

			listRs = mGetParentsStmt.getResultSet();
			if (listRs.next()) {
				parentsList = listRs.getString(1);
			}

			if (listRs != null) {
				listRs.close();
			}

			parsedParentsList = parentsList.split(",");
			parents = new int[parsedParentsList.length];
			for (String parent : parsedParentsList) {
				if (parent.length() != 0)
					parents[counter] = Integer.parseInt(parent);
				else
					parents[counter] = -1;
				counter++;
			}

			return parents;
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return new int[0];
		}
	}

	public int getPackageIdFromName(String name) {
		if (!mIsConnected) return -1;
		try {
			ResultSet idRs = null;
			int id = -1;

			mGetPackageIdStmt.clearParameters();
			mGetPackageIdStmt.setString(1, name);

			mGetPackageIdStmt.execute();

			idRs = mGetPackageIdStmt.getResultSet();
			if (idRs.next()) {
				id = idRs.getInt(1);
			}

			if (idRs != null) {
				idRs.close();
			}

			return id;
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return -1;
		}
	}

	private int getGlobalSourceIdFromName(String name) {
		if (!mIsConnected) return -1;
		try {
			ResultSet idRs = null;
			int id = -1;

			mGetGlobalSourceIdStmt.clearParameters();
			mGetGlobalSourceIdStmt.setString(1, name);

			mGetGlobalSourceIdStmt.execute();

			/*
			 * There may be multiple. However, in the world of java,
			 * package . class name should be unique.
			 */
			idRs = mGetGlobalSourceIdStmt.getResultSet();
			if (idRs.next()) {
				id = idRs.getInt(1);
			}

			if (idRs != null) {
				idRs.close();
			}

			return id;
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return -1;
		}
	}

	public String getSourceFromSourceId(int sourceId) {
		if (!mIsConnected) return null;
		try {
			ResultSet sourceRs = null;
			String source = null;

			mGetSourceStmt.clearParameters();
			mGetSourceStmt.setInt(1, sourceId);

			mGetSourceStmt.execute();

			sourceRs = mGetSourceStmt.getResultSet();
			if (sourceRs.next()) {
				source = sourceRs.getString(1);
			}

			if (sourceRs != null) {
				sourceRs.close();
			}

			return source;
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return null;
		}
	}

	public int getMemberIdFromSourceId(int sourceId) {
		if (!mIsConnected) return -1;
		try {
			ResultSet memberRs = null;
			int memberId = -1;

			mGetMemberIdStmt.clearParameters();
			mGetMemberIdStmt.setInt(1, sourceId);

			mGetMemberIdStmt.execute();

			memberRs = mGetMemberIdStmt.getResultSet();
			if (memberRs.next()) {
				memberId = memberRs.getInt(1);
			}

			if (memberRs != null) {
				memberRs.close();
			}

			return memberId;
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return -1;
		}
	}

	public int getSourceIdFromName(int packageId,String name,int parameterCount) {
		if (!mIsConnected) return -1;
		try {
			ResultSet idRs = null;
			int id = -1;

			mGetSourceIdStmt.clearParameters();
			mGetSourceIdStmt.setInt(1, packageId);
			mGetSourceIdStmt.setString(2, name);
			mGetSourceIdStmt.setInt(3, parameterCount);

			mGetSourceIdStmt.execute();

			idRs = mGetSourceIdStmt.getResultSet();
			if (idRs.next()) {
				id = idRs.getInt(1);
			}

			if (idRs != null) {
				idRs.close();
			}

			return id;
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return -1;
		}
	}

	public boolean addParentName(int packageId, int sourceId, String parentName) {
		if (!mIsConnected) return false;
		int existingParentId = -1;

		existingParentId = getGlobalSourceIdFromName(parentName);

		if (existingParentId != -1) {
			return addParentId(packageId, sourceId, existingParentId);
		}

		try {
			mInsertParentNameStmt.clearParameters();
			mInsertParentNameStmt.setInt(1, packageId);
			mInsertParentNameStmt.setInt(2, sourceId);
			mInsertParentNameStmt.setString(3, parentName);

			/*
			 * Yes, this returns a boolean, but it's meaning
			 * is useless in determining if this statement
			 * was successful. Disregard.
			 */
			mInsertParentNameStmt.execute();

			return true;
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return false;
		}
	}

	public boolean addParentId(int packageId, int sourceId, int parentId) {
		if (!mIsConnected) return false;
		try {
			mInsertParentIdStmt.clearParameters();
			mInsertParentIdStmt.setInt(1, packageId);
			mInsertParentIdStmt.setInt(2, sourceId);
			mInsertParentIdStmt.setInt(3, parentId);

			/*
			 * Yes, this returns a boolean, but it's meaning
			 * is useless in determining if this statement
			 * was successful. Disregard.
			 */
			mInsertParentIdStmt.execute();
			return true;
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return false;
		}
	}

	public boolean updateParentId(String parentName, int parentId) {
		if (!mIsConnected) return false;
		try {
			System.err.println("Attempting to update parent: " + parentName + " -> " + parentId);

			mUpdateParentIdStmt.clearParameters();
			mUpdateParentIdStmt.setInt(1, parentId);
			mUpdateParentIdStmt.setString(2, parentName);

			/*
			 * Yes, this returns a boolean, but it's meaning
			 * is useless in determining if this statement
			 * was successful. Disregard.
			 */
			mUpdateParentIdStmt.execute();

			return true;
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return false;
		}
	}

	public int addPackage(String name, String filename, String url) {
		if (!mIsConnected) return -1;
		try {
			ResultSet aiRs = null;
			int key = -1;

			mInsertPackageStmt.clearParameters();
			mInsertPackageStmt.setString(1, name);
			mInsertPackageStmt.setString(2, filename);
			mInsertPackageStmt.setString(3, url);

			/*
			 * Yes, this returns a boolean, but it's meaning
			 * is useless in determining if this statement
			 * was successful. Disregard.
			 */
			mInsertPackageStmt.execute();

			return getAutoId(mInsertPackageStmt);
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return -1;
		}
	}

	public int addDependencyName(int packageId, int sourceId, String dependsOn) {
		if (!mIsConnected) return -1;
		int existingDependencyId = -1;

		existingDependencyId = getGlobalSourceIdFromName(dependsOn);

		if (existingDependencyId != -1) {
			return addDependencyId(packageId, sourceId, existingDependencyId);
		}

		try {
			ResultSet aiRs = null;
			int key = -1;

			mInsertDependencyNameStmt.clearParameters();
			mInsertDependencyNameStmt.setInt(1, packageId);
			mInsertDependencyNameStmt.setInt(2, sourceId);
			mInsertDependencyNameStmt.setString(3, dependsOn);

			/*
			 * Yes, this returns a boolean, but it's meaning
			 * is useless in determining if this statement
			 * was successful. Disregard.
			 */
			mInsertDependencyNameStmt.execute();

			return getAutoId(mInsertDependencyNameStmt);
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return -1;
		}
	}

	public int addDependencyId(int packageId, int sourceId, int dependsOn) {
		if (!mIsConnected) return -1;
		try {
			ResultSet aiRs = null;
			int key = -1;

			mInsertDependencyIdStmt.clearParameters();
			mInsertDependencyIdStmt.setInt(1, packageId);
			mInsertDependencyIdStmt.setInt(2, sourceId);
			mInsertDependencyIdStmt.setInt(3, dependsOn);

			/*
			 * Yes, this returns a boolean, but it's meaning
			 * is useless in determining if this statement
			 * was successful. Disregard.
			 */
			mInsertDependencyIdStmt.execute();

			return getAutoId(mInsertDependencyIdStmt);
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return -1;
		}
	}

	public int addSource(int packageId, String type, int memberId, String returnType, String name, int parameterCount, String code) {
		if (!mIsConnected) return -1;
		try {
			mInsertSourceStmt.clearParameters();
			mInsertSourceStmt.setInt(1, packageId);
			mInsertSourceStmt.setString(2, type);
			mInsertSourceStmt.setInt(3, memberId);
			mInsertSourceStmt.setString(4, returnType);
			mInsertSourceStmt.setString(5, name);
			mInsertSourceStmt.setInt(6, parameterCount);
			mInsertSourceStmt.setString(7, code);

			/*
			 * Yes, this returns a boolean, but it's meaning
			 * is useless in determining if this statement
			 * was successful. Disregard.
			 */
			mInsertSourceStmt.execute();

			return getAutoId(mInsertSourceStmt);
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return -1;
		}
	}

	public boolean updateDependencyId(String dependsOnName, int dependsOnId) {
		if (!mIsConnected) return false;
		try {
			System.err.println("Attempting to update dependency: " + dependsOnName + " -> " + dependsOnId);

			mUpdateDependencyIdStmt.clearParameters();
			mUpdateDependencyIdStmt.setInt(1, dependsOnId);
			mUpdateDependencyIdStmt.setString(2, dependsOnName);

			/*
			 * Yes, this returns a boolean, but it's meaning
			 * is useless in determining if this statement
			 * was successful. Disregard.
			 */
			mUpdateDependencyIdStmt.execute();

			return true;
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return false;
		}
	}


	public boolean updateSource(int packageId, int sourceId, String source) {
		if (!mIsConnected) return false;
		try {
			mUpdateSourceStmt.clearParameters();
			mUpdateSourceStmt.setString(1, source);
			mUpdateSourceStmt.setInt(2, packageId);
			mUpdateSourceStmt.setInt(3, sourceId);

			/*
			 * Yes, this returns a boolean, but it's meaning
			 * is useless in determining if this statement
			 * was successful. Disregard.
			 */
			mUpdateSourceStmt.execute();

			return true;
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return false;
		}
	}

	public int addDocumentation(int packageId, int sourceId, String doc) {
		if (!mIsConnected) return -1;

		try {
			mInsertDocumentationStmt.clearParameters();
			mInsertDocumentationStmt.setInt(1, packageId);
			mInsertDocumentationStmt.setInt(2, sourceId);
			mInsertDocumentationStmt.setString(3, doc);

			mInsertDocumentationStmt.execute();

			return getAutoId(mInsertDocumentationStmt);
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return -1;
		}
	}

	public int addParameter(int packageId,
		int sourceId,
		String type,
		String name) {
		if (!mIsConnected) return -1;

		try {
			mInsertParameterStmt.clearParameters();
			mInsertParameterStmt.setInt(1, packageId);
			mInsertParameterStmt.setInt(2, sourceId);
			mInsertParameterStmt.setString(3, type);
			mInsertParameterStmt.setString(4, name);

			mInsertParameterStmt.execute();

			return getAutoId(mInsertParameterStmt);
		} catch (SQLException e) {
			System.err.println("Warning: SQL exception: " + e.toString());
			return -1;
		}
	}

	public boolean disconnect() {
		if (mIsConnected) {
			try {
				mSqlConnection.close();
				mIsConnected = false;
				return true;
			} catch (SQLException e) {
				System.out.println("Error occurred disconnecting from DocDb: " + 
					e.toString());
				return false;
			}
		}
		return true;
	}
}
