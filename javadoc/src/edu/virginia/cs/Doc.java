package edu.virginia.cs;

import com.sun.javadoc.Doclet;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ConstructorDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.DocErrorReporter;
import java.lang.System;

public class Doc extends Doclet {

	private static String oMysqlHost = null, 
		oMysqlUser = null,
		oMysqlPass = null,
		oMysqlDb = null;
	private static String mMysqlHost = null, 
		mMysqlUser = null,
		mMysqlPass = null,
		mMysqlDb = null;

	public Doc(String mysqlHost,
		String mysqlUser,
		String mysqlPass,
		String mysqlDb) {
		mMysqlHost = mysqlHost;
		mMysqlUser = mysqlUser;
		mMysqlPass = mysqlPass;
		mMysqlDb = mysqlDb;
	}

	public static boolean start(RootDoc root) {
		Doc doc = new Doc(oMysqlHost, oMysqlUser, oMysqlPass, oMysqlDb);

		return doc.root(root);
	}

	public boolean root(RootDoc doc) {
		for (ClassDoc classDoc : doc.classes()) {
			handleClass(classDoc, 0);
	 }
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
				} 
			}
		}
		if (oMysqlHost != null && 
			oMysqlUser != null &&
			oMysqlPass != null &&
			oMysqlDb != null) {
			return true;
		}
		return false;
	}

	private static void handleClass(ClassDoc classDoc, int indent) {
		System.out.println("class: " + classDoc.toString());
		for (ConstructorDoc constructorDoc : classDoc.constructors()) {
			printIndent(indent+1);
			System.out.println("constructor: " + constructorDoc.toString());
		}
		for (MethodDoc methodDoc : classDoc.methods()) {
			printIndent(indent+1);
			System.out.println("method: " + methodDoc.toString());
		}
		for (FieldDoc fieldDoc : classDoc.fields()) {
			printIndent(indent+1);
			System.out.println("field: " + fieldDoc.toString());
		}
		for (ClassDoc innerClass : classDoc.innerClasses()) {
			handleClass(innerClass, indent+1);
		}
	}

	private static void printIndent(int indent) {
		for (int i = 0; i<indent; i++) { System.out.print("\t"); }
	}
}
