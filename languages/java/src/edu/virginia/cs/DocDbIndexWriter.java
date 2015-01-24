package edu.virginia.cs;

import org.apache.lucene.util.Version;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;
import java.io.File;
import java.io.IOException;
import edu.virginia.cs.DocDb;

public class DocDbIndexWriter {
	
	private static String mMysqlHost = null, 
		mMysqlUser = null,
		mMysqlPass = null,
		mMysqlDb = null,
		mIndexFile = null;
	private static DocDb mDocDb = null;

	public static void main(String params[]) {
		File methodIndexPath = null,
			classIndexPath = null;
		IndexWriterConfig methodIndexWriterConfig = null,
			classIndexWriterConfig = null;
		IndexWriter methodIndexWriter = null,
			classIndexWriter = null;
		StandardAnalyzer standardAnalyzer = null;

		for (int i = 0; i<params.length; i++) {
			String param = params[i];
			System.out.println("param: " + param);
			if (param.equalsIgnoreCase("-mysqlhost")) {
				mMysqlHost = params[i+1];
				i++;
			} else if (param.equalsIgnoreCase("-mysqluser")) {
				mMysqlUser = params[i+1];
				i++;
			} else if (param.equalsIgnoreCase("-mysqlpass")) {
				mMysqlPass = params[i+1];
				i++;
			} else if (param.equalsIgnoreCase("-mysqldb")) {
				mMysqlDb = params[i+1];
				i++;
			}	else if (param.equalsIgnoreCase("-index")) {
				mIndexFile = params[i+1];
				i++;
			}
		}

		if (mMysqlHost == null ||
			mMysqlUser == null ||
			mMysqlPass == null ||
			mMysqlDb == null ||
			mIndexFile == null) {
			System.err.println("All parameters required: mysqlhost, " +
				"mysqluser, " +
				"mysqlpass, " +
				"mysqldb, and " +
				"index.");
			return;
		}

		mDocDb = new DocDb(mMysqlHost, mMysqlUser, mMysqlPass, mMysqlDb);
		if (!mDocDb.connect()) {
			System.err.println("Could not connect to the DocDb.");
			return;
		}

		standardAnalyzer = new StandardAnalyzer();

		methodIndexPath = new File(mIndexFile + "-method");
		methodIndexWriterConfig = new IndexWriterConfig(Version.LATEST, standardAnalyzer);
		classIndexPath = new File(mIndexFile + "-class");
		classIndexWriterConfig = new IndexWriterConfig(Version.LATEST, standardAnalyzer);

		try {
			classIndexWriter = new IndexWriter(
				FSDirectory.open(classIndexPath),
				classIndexWriterConfig);
			methodIndexWriter = new IndexWriter(
				FSDirectory.open(methodIndexPath),
				methodIndexWriterConfig);
		} catch (IOException ioex) {
			System.err.println("Could not open index file: " + ioex.toString());
			return;
		}

		/*
		 * Do the indexing.
		 */
		int counter = 0;
		int sourceIds[] = mDocDb.getSourceIds();
		int sourceIdsOfMethods[] = mDocDb.getSourceIdsOfMethods();
		int sourceIdsOfClasses[] = mDocDb.getSourceIdsOfClasses();

		System.err.println("Total method ids: " + sourceIdsOfMethods.length);
		for (int sourceId : sourceIdsOfMethods) {
			String documentation = mDocDb.getDocumentationFromSourceId(sourceId);
			String source = mDocDb.getSourceFromSourceId(sourceId);
			String name = mDocDb.getNameFromSourceId(sourceId);
			String variables = mDocDb.getVariablesFromSourceId(sourceId);
			String invocations = mDocDb.getInvocationsFromSourceId(sourceId);
			int memberId = mDocDb.getMemberIdFromSourceId(sourceId);

			/*
			 * Generate a new document.
			 */
			Document d = new Document();
			/*
			 * Add the fields.
			 */
			d.add(new IntField("id", sourceId, Field.Store.YES));
			d.add(new IntField("memberId", memberId, Field.Store.YES));
			d.add(new TextField("name", name, Field.Store.YES));
			if (source != null && source.length() > 0)
				d.add(new TextField("source", source, Field.Store.YES));
			if (documentation != null && documentation.length() > 0)
				d.add(new TextField("documentation", documentation, Field.Store.YES));
			if (variables != null && variables.length() > 0)
				d.add(new TextField("variables", variables, Field.Store.YES));
			if (invocations != null && variables.length() > 0)
				d.add(new TextField("invocations", invocations, Field.Store.YES));
			/*
			 * Put it in the index.
			 */
			try {
				methodIndexWriter.addDocument(d);
			} catch (IOException ioex) {
				System.err.println("IO Error writing to index: " + ioex.toString());
			}
		}

		System.err.println("Total class ids: " + sourceIdsOfClasses.length);
		for (int sourceId : sourceIdsOfClasses) {
			int parents[] = mDocDb.getParentsFromId(sourceId);
			String documentation = mDocDb.getDocumentationFromSourceId(sourceId);
			String name = mDocDb.getNameFromSourceId(sourceId);

			assert(parents.length <= 1);

			/*
			 * Generate a new document.
			 */
			Document d = new Document();
			/*
			 * Add the fields.
			 */
			d.add(new IntField("id", sourceId, Field.Store.YES));
			if (parents.length == 1)
				d.add(new IntField("id", parents[0], Field.Store.YES));
			else
				d.add(new IntField("id", -1, Field.Store.YES));
			d.add(new TextField("name", name, Field.Store.YES));
			if (documentation != null && documentation.length() > 0)
				d.add(new TextField("documentation", documentation, Field.Store.YES));
			/*
			 * Put it in the index.
			 */
			try {
				classIndexWriter.addDocument(d);
			} catch (IOException ioex) {
				System.err.println("IO Error writing to index: " + ioex.toString());
			}
		}
		try {
			methodIndexWriter.close();
			classIndexWriter.close();
		} catch (IOException ioex) {
			System.err.println("Could not close index: " + ioex.toString());
		}

		mDocDb.disconnect();
	}
}
