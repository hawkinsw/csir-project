package edu.virginia.cs;

import com.sun.javadoc.Doclet;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ConstructorDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.DocErrorReporter;
import java.lang.System;
import edu.virginia.cs.DocDb;

public class Doc extends Doclet {

	private static String oMysqlHost = null, 
		oMysqlUser = null,
		oMysqlPass = null,
		oMysqlDb = null,
		oSourceName = null,
		oSourceUrl = null,
		oSourcePackage = null;

	private int mPackageId = -1;

	private String mSourceName = null,
		mSourceUrl = null,
		mSourcePackage = null;
	private DocDb mDb = null;

	public Doc(String mysqlHost,
		String mysqlUser,
		String mysqlPass,
		String mysqlDb,
		String sourceName,
		String sourceUrl,
		String sourcePackage) {
		mDb = new DocDb(mysqlHost, mysqlUser, mysqlPass, mysqlDb);

		mSourceName = sourceName;
		mSourceUrl = sourceUrl;
		mSourcePackage = sourcePackage;
	}

	public static boolean start(RootDoc root) {
		Doc doc = new Doc(oMysqlHost, oMysqlUser, oMysqlPass, oMysqlDb,
			oSourceName, oSourceUrl, oSourcePackage);

		return doc.root(root); 
	}

	public boolean root(RootDoc doc) {
		if (!mDb.connect())
			return false;

		mPackageId = mDb.getPackageIdFromName(mSourceName);
		if (mPackageId == -1) {
			mPackageId = mDb.addPackage(mSourceName, mSourcePackage, mSourceUrl);
		}

		if (mPackageId == -1) {
			mDb.disconnect();
			return false;
		}

		for (ClassDoc classDoc : doc.classes()) {
			handleClass(classDoc, 0);
		}
/*
		int packageId = mDb.addPackage("a", "b", "c");
		System.out.println("Package ID: " + packageId);

		int sourceId = mDb.addSource(packageId, "method", "main", "return 0;");
		System.out.println("Source ID: " + sourceId);

		int documentationId = mDb.addDocumentation(packageId,sourceId,"Document1");
		System.out.println("Documentation ID: " + documentationId);

		packageId = mDb.getPackageIdFromName("a");
		System.out.println("(Fetched) Package ID: " + packageId);
*/
		mDb.disconnect();
		return true;
	}

	public static int optionLength(String option) {
		if (option.equalsIgnoreCase("-mysqlhost")) {
			return 2;
		} else if (option.equalsIgnoreCase("-mysqluser")) {
			return 2;
		} else if (option.equalsIgnoreCase("-mysqlpass")) {
			return 2;
		} else if (option.equalsIgnoreCase("-mysqldb")) {
			return 2;
		} else if (option.equalsIgnoreCase("-sourcename")) {
			return 2;
		} else if (option.equalsIgnoreCase("-sourceurl")) {
			return 2;
		} else if (option.equalsIgnoreCase("-sourcepackage")) {
			return 2;
		}
		return 0;
	}
	public static boolean validOptions(String[][] options, 
		DocErrorReporter reporter)
	{
		for (String[] o : options) {
			if (o.length > 1) {
				if (o[0].equalsIgnoreCase("-mysqlhost")) {
					oMysqlHost = o[1];
				} else if (o[0].equalsIgnoreCase("-mysqluser")) {
					oMysqlUser = o[1];
				} else if (o[0].equalsIgnoreCase("-mysqlpass")) {
					oMysqlPass = o[1];
				} else if (o[0].equalsIgnoreCase("-mysqldb")) {
					oMysqlDb = o[1];
				} else if (o[0].equalsIgnoreCase("-sourcename")) {
					oSourceName = o[1];
				} else if (o[0].equalsIgnoreCase("-sourceurl")) {
					oSourceUrl = o[1];
				} else if (o[0].equalsIgnoreCase("-sourcepackage")) {
					oSourcePackage = o[1];
				} 
			}
		}
		if (oMysqlHost != null && 
			oMysqlUser != null &&
			oMysqlPass != null &&
			oMysqlDb != null &&
			oSourceName != null &&
			oSourcePackage != null &&
			oSourceUrl != null) {
			return true;
		}
		return false;
	}

	private void handleFields(FieldDoc fieldDocs[], int indent) {
		for (FieldDoc fieldDoc : fieldDocs) {
			printIndent(indent+1);
			System.out.println("field: " + fieldDoc.toString());
		}
	}

	/**
	 * handleMethods()
	 *
	 * Parse method comments and insert them into the database.
	 */
	private void handleMethods(int classId, MethodDoc methodDocs[], int indent) {
		for (MethodDoc methodDoc : methodDocs) {
			printIndent(indent+1);
			System.out.println("method: " + methodDoc.qualifiedName());
			int sourceId = mDb.addSource(mPackageId,
				"method",
				classId,
				methodDoc.returnType().toString(),
				methodDoc.qualifiedName(),
				methodDoc.parameters().length,
				"");

			if (sourceId != -1)
				mDb.addDocumentation(mPackageId,sourceId,methodDoc.commentText());
		}
	}

	private void handleClass(ClassDoc classDoc, int indent) {
		System.out.println("class: " + classDoc.toString());

		/*
		 * Add the class itself!
		 */
		int classId = mDb.addSource(mPackageId,
			"class",
			0,
			"",
			classDoc.qualifiedName(),
			0,
			"");
		if (classId != -1) {
			mDb.addParentName(mPackageId, classId, classDoc.superclass().qualifiedName());
		}

		for (ConstructorDoc constructorDoc : classDoc.constructors()) {
			printIndent(indent+1);
			System.out.println("constructor: " + constructorDoc.qualifiedName());

			int sourceId = mDb.addSource(mPackageId,
				"method",
				classId,
				classDoc.qualifiedName(),
				constructorDoc.qualifiedName(),
				constructorDoc.parameters().length,
				"");

			if (sourceId != -1)
				mDb.addDocumentation(mPackageId,sourceId,constructorDoc.commentText());
		}
		if (true) {
			handleMethods(classId, classDoc.methods(), indent+1);
		}

		if (false) {
			/*
			 * We are not handling fields at this point.
			 */
			handleFields(classDoc.fields(), indent+1);
		}

		for (ClassDoc innerClass : classDoc.innerClasses()) {
			handleClass(innerClass, indent+1);
		}
	}

	private void printIndent(int indent) {
		for (int i = 0; i<indent; i++) { System.out.print("\t"); }
	}
}
