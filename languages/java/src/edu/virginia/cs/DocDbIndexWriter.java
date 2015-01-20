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
		File indexPath = null;
		IndexWriterConfig indexWriterConfig = null;
		IndexWriter indexWriter = null;
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

		indexPath = new File(mIndexFile);
		standardAnalyzer = new StandardAnalyzer();
		indexWriterConfig = new IndexWriterConfig(Version.LATEST, standardAnalyzer);

		try {
			indexWriter = new IndexWriter(
				FSDirectory.open(indexPath),
				indexWriterConfig);
		} catch (IOException ioex) {
			System.err.println("Could not open index file: " + ioex.toString());
			return;
		}

		/*
		 * Do the indexing.
		 */
		int counter = 0;
		int sourceIds[] = mDocDb.getSourceIds();
		System.err.println("Total source ids: " + sourceIds.length);
		for (int sourceId : sourceIds) {
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
			d.add(new StringField("name", name, Field.Store.YES));
			d.add(new TextField("source", source, Field.Store.NO));
			d.add(new TextField("documentation", source, Field.Store.NO));
			d.add(new TextField("variables", variables, Field.Store.YES));
			d.add(new TextField("invocations", invocations, Field.Store.YES));

			/*
			 * Put it in the index.
			 */
			try {
				indexWriter.addDocument(d);
			} catch (IOException ioex) {
				System.err.println("IO Error writing to index: " + ioex.toString());
			}
		}
		try {
			indexWriter.close();
		} catch (IOException ioex) {
			System.err.println("Could not close index: " + ioex.toString());
		}

		mDocDb.disconnect();
	}
}
