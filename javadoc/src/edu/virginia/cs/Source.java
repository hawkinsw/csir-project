package edu.virginia.cs;

import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.source.util.JavacTask;
import com.sun.source.tree.Tree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;

import java.util.Set;
import java.util.List;
import java.util.LinkedList;
import java.io.IOException;
import java.io.File;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.element.Name;
import javax.lang.model.util.Elements;
import javax.lang.model.util.ElementScanner6;
import javax.lang.model.type.TypeKind;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.ToolProvider;

import edu.virginia.cs.DocDb;

@SupportedAnnotationTypes("*")
public class Source extends AbstractProcessor {
	private static String oMysqlHost = null, 
		oMysqlUser = null,
		oMysqlPass = null,
		oMysqlDb = null,
		oSourceName = null,
		oSourceUrl = null,
		oSourcePackage = null,
		oFile = null;

	private String mSourceName = null,
		mSourcePackage = null,
		mSourceUrl = null,
		mMysqlHost = null,
		mMysqlUser = null,
		mMysqlPass = null,
		mMysqlDb = null;

	public static void main(String params[]) {

		/*
		 * Need a compiler, a file manager and some files.
		 *
		 * Then I put them into a processor list. Give that
		 * to the processor and then GO!
		 */
		for (int i = 0; i<params.length; i++) {
			String param = params[i];
			System.out.println("param: " + param);
			if (param.equalsIgnoreCase("-mysqlhost")) {
				oMysqlHost = params[i+1];
				i++;
			} else if (param.equalsIgnoreCase("-mysqluser")) {
				oMysqlUser = params[i+1];
				i++;
			} else if (param.equalsIgnoreCase("-mysqlpass")) {
				oMysqlPass = params[i+1];
				i++;
			} else if (param.equalsIgnoreCase("-mysqldb")) {
				oMysqlDb = params[i+1];
				i++;
			} else if (param.equalsIgnoreCase("-sourcename")) {
				oSourceName = params[i+1];
				i++;
			} else if (param.equalsIgnoreCase("-sourceurl")) {
				oSourceUrl = params[i+1];
				i++;
			} else if (param.equalsIgnoreCase("-sourcepackage")) {
				oSourcePackage = params[i+1];
				i++;
			} else if (param.equalsIgnoreCase("-file")) {
				oFile = params[i+1];
				System.out.println("oFile: " + oFile);
				i++;
			}
		}

		Source sourceProcessor = new Source(oSourceName, oSourcePackage, oSourceUrl, oMysqlHost, oMysqlUser, oMysqlPass, oMysqlDb);
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(
			null, null, null);
		LinkedList<File> files = new LinkedList<File>();

		/* for (String fileName : params) { */
		if (true) {
			File f = new File(oFile);

			if (f != null) {
				files.add(f);
			}
			else
			{
				System.out.println("Skipping "+oFile+" - could not make a file.");
			}
		}

		Iterable<? extends JavaFileObject> fileObjects = 
			fileManager.getJavaFileObjectsFromFiles(files);
		CompilationTask task = 
			compiler.getTask(null, fileManager, null, null, null, fileObjects);

		LinkedList<Source> processors = new LinkedList<Source>();
		processors.add(sourceProcessor);
		task.setProcessors(processors);

		task.call();

		try {
			fileManager.close();
		} catch (IOException ioEx) {
			System.out.println("Error closing file manager.");
		}
	}

	private Elements mElementUtils = null;

	public Source(String sourcePackage, String sourceName, String sourceUrl, String mysqlHost, String mysqlUser, String mysqlPass, String mysqlDb) {
		super();
		mSourcePackage = sourcePackage;
		mSourceName = sourceName;
		mSourceUrl = sourceUrl;
		mMysqlHost = mysqlHost;
		mMysqlUser = mysqlUser;
		mMysqlPass = mysqlPass;
		mMysqlDb = mysqlDb;
	}

	public void init(ProcessingEnvironment pe) {
		super.init(pe);
		mElementUtils = pe.getElementUtils();
		m_trees = Trees.instance(pe);	
	}

	public boolean process(Set<? extends TypeElement> annotations, 
		RoundEnvironment re) {
		SourceVisitor visitor = new SourceVisitor();
		int packageId = -1;
		DocDb db = new DocDb(mMysqlHost, mMysqlUser, mMysqlPass, mMysqlDb);

		if (!db.connect()) return false;

		System.err.println("Looking for " + mSourcePackage);
		packageId = db.getPackageIdFromName(mSourcePackage);
		System.err.println("packageId: " + packageId);
		if (packageId == -1) {
			db.disconnect();
			return false;
		}

		for (Element e : re.getRootElements()) {
			TreePath treePath = m_trees.getPath(e);
			DependencyVisitor dependencyVisitor = new DependencyVisitor();

			dependencyVisitor.scan(e, null);

			visitor.setDocDb(db);
			visitor.setPackageId(packageId);
			visitor.setQualification(mElementUtils.getPackageOf(e).toString());
			visitor.setDependencies(dependencyVisitor.getDependencies());

			visitor.scan(treePath, m_trees);
		}

		db.disconnect();
		return true;
	}

	private Trees m_trees;

	private class DependencyVisitor extends ElementScanner6<Void,Void> {
		private List<String> mDependencies = null;
		{
			mDependencies = new LinkedList<String>();
		}
		public Void visitVariable(VariableElement variableElement, Void v) {
			String variableTypeName = variableElement.asType().toString();
			TypeKind variableTypeKind = variableElement.asType().getKind();
			if (!variableTypeKind.isPrimitive() &&
			    !variableTypeName.startsWith("java.") &&
			    !variableTypeName.startsWith("com.sun.") &&
					!variableTypeName.startsWith("javax.")) {
				mDependencies.add(variableTypeName);
			}
			return super.visitVariable(variableElement, v);
		}
		public List<String> getDependencies() {
			return mDependencies;
		}
	}

	private class SourceVisitor extends TreePathScanner<Object,Trees> {
		private String mQualification = null;
		private DocDb mDb = null;
		private int mPackageId = -1;
		private String mClass = null;
		private List<String> mDependencies = null;

		public void setQualification(String qualifier) {
			mQualification = qualifier;
			mDependencies = new LinkedList<String>();
		}

		public void setDocDb(DocDb db) {
			mDb = db;
		}

		public void setPackageId(int id) {
			mPackageId = id;
		}

		public void setDependencies(List<String> dependencies) {
			mDependencies = dependencies;
		}

		public Object visitClass(ClassTree classNode, Trees trees) {
			mClass = classNode.getSimpleName().toString();

			if (mQualification != null && mDb != null && mPackageId != -1) {
				int sourceId = mDb.getSourceIdFromName(mPackageId, mQualification + "." + mClass);
				if (sourceId != -1) {
					/*
					 * update any dependencies that might point here.
					 */
					mDb.updateDependencyId(mClass, sourceId);

					/*
					 * Create any dependencies that might point from here.
					 */
					for (String dependencyName : mDependencies) {
						System.err.println("Adding dependency on " + dependencyName);
						mDb.addDependencyName(mPackageId, sourceId, dependencyName);
					}
				}
			}
			return super.visitClass(classNode, trees);
		}

		public Object visitMethod(MethodTree methodNode, Trees trees) {
			if (mQualification != null && mDb != null && mPackageId != -1) {
				int sourceId = -1;
				BlockTree b = methodNode.getBody();
				String body = null;
				String methodName = null;

				methodName = mQualification + "." + mClass + "." + methodNode.getName();
				System.err.println("Method: " + methodName);

				sourceId = mDb.getSourceIdFromName(mPackageId, methodName);

				if (sourceId != -1) {
					body = new String();
					for (StatementTree statement : b.getStatements()) {
						body += statement.toString() + "\n";
					}
					mDb.updateSource(mPackageId, sourceId, body);

					for (VariableTree v : methodNode.getParameters()) {
						String parameterType = v.getType().toString();
						String parameterName = v.getName().toString();

						mDb.addParameter(mPackageId,sourceId,parameterType,parameterName);
					}
				} else {
					System.err.println("Warning: Could not update source for method " + methodName + ".");
				}

				/*
				System.out.println(body);
				 */
			}
			return super.visitMethod(methodNode, trees);
		}
	}
}
