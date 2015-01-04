package edu.virginia.cs;

import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.source.util.JavacTask;
import com.sun.source.tree.Tree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.CompilationUnitTree;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Collections;
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
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.Name;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.lang.model.util.ElementScanner6;
import javax.lang.model.type.TypeKind;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.ToolProvider;

import edu.virginia.cs.DocDb;

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

	public Set<String> getSupportedAnnotationTypes() {
		HashSet<String> annotationTypes = new HashSet<String>();
		annotationTypes.add("*");
		return annotationTypes;
	}

	public void init(ProcessingEnvironment pe) {
		super.init(pe);
		mElementUtils = pe.getElementUtils();
		m_typeUtils = pe.getTypeUtils();
		m_trees = Trees.instance(pe);	
	}

	public boolean process(Set<? extends TypeElement> annotations, 
		RoundEnvironment re) {
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
			ExecutableVisitor visitor = new ExecutableVisitor();
			ClassVisitor classVisitor = new ClassVisitor();
			TreePath treePath = m_trees.getPath(e);
			List<String> dependencies = null;
			List<String> imports = null;

			imports = calculateImportsFromPath(treePath);
			dependencies = calculateDependenciesFromPath(treePath);

			classVisitor.setDocDb(db);
			classVisitor.setPackageId(packageId);
			classVisitor.setDependencies(dependencies);
			classVisitor.setImports(imports);
			classVisitor.scan(treePath, e.getEnclosingElement().toString());

			visitor.setDocDb(db);
			visitor.setPackageId(packageId);
			visitor.setUtils(mElementUtils);
			visitor.scan(e, m_trees);
		}

		db.disconnect();
		return true;
	}

	private Trees m_trees;
	private Types m_typeUtils;

	private List<String> calculateImportsFromPath(TreePath tp) {
		CompilationUnitTree cpuTree = tp.getCompilationUnit();
		LinkedList<String> imports = new LinkedList<String>();
		for (ImportTree importTree : cpuTree.getImports()) {
			String importName = importTree.getQualifiedIdentifier().toString();
			imports.add(importName);
		}
		Collections.sort(imports);
		return imports;
	}

	private List<String> calculateDependenciesFromPath(TreePath tp) {
		LinkedList<String> dependencies = new LinkedList<String>();
		for (String importName : calculateImportsFromPath(tp)) {
			if (!importName.startsWith("java.") &&
			    !importName.startsWith("com.sun.") &&
			    !importName.startsWith("javax."))
				dependencies.add(importName);
		}
		return dependencies;
	}

	private class ClassVisitor extends TreePathScanner<Object,String> {
		private List<String> mDependencies = null;
		private List<String> mImports = null;
		private int mPackageId = -1;
		private DocDb mDb = null;

		public void setDependencies(List<String> dependencies) {
			mDependencies = dependencies;
		}

		public void setImports(List<String> imports) {
			mImports = imports;
		}
		public void setPackageId(int packageId) {
			mPackageId = packageId;
		}

		public void setDocDb(DocDb db) {
			mDb = db;
		}

		public Object visitClass(ClassTree classNode, String nameQualification) {
			String clazz = classNode.getSimpleName().toString();
			boolean hasParent = false;
			String parentName = null;

			/*
			 * Find the full name of the class being extended, if applicable.
			 */
			if (classNode.getExtendsClause() != null) {
				/*
				 * Get the basic name first.
				 */
				Tree extendsClauseTree = classNode.getExtendsClause();
				String extendsClauseName = "";
				if (extendsClauseTree.getKind() == Tree.Kind.IDENTIFIER) {
					IdentifierTree extendsClauseIdentifierTree = 
						(IdentifierTree)extendsClauseTree;
					extendsClauseName = extendsClauseIdentifierTree.getName().toString();
				} else if(extendsClauseTree.getKind() == Tree.Kind.PARAMETERIZED_TYPE){
					ParameterizedTypeTree extendsClausePTree = 
						(ParameterizedTypeTree)extendsClauseTree;

					extendsClauseName = extendsClausePTree.getType().toString();
				}
				/*
				 * Iterate through the imported packages, to resolve
				 * this to a fully-qualified name.
				 */
				for (String importName : mImports) {
					if (importName.endsWith(extendsClauseName)) {
						parentName = importName;
						hasParent = true;
						break;
					}
				}
			}

			/*
			 * Skip anonymous inner classes.
			 */
			if (clazz.equals("")) {
				System.err.println("Skipping anonymous inner classes.");
				return super.visitClass(classNode, nameQualification);
			}

			clazz = nameQualification + "." + clazz;
			System.err.println("Visiting class: " + clazz);

			if (mDb != null && mPackageId != -1) {
				int sourceId = mDb.getSourceIdFromName(mPackageId, clazz, 0);
				if (sourceId == -1) {
					/*
					 * Put in this source if we need to.
					 */
					sourceId = mDb.addSource(mPackageId, "class", "", clazz, 0, "");
				}
				/*
				 * Even though we just added it, there could have
				 * been a database error.
				 */
				if (sourceId != -1) {
					/*
					 * update any dependencies that might point here.
					 */
					mDb.updateDependencyId(clazz, sourceId);
					/*
					 * Create any dependencies that might point from here.
					 */
					for (String dependencyName : mDependencies) {
						System.err.println("Adding dependency on " + dependencyName);
						mDb.addDependencyName(mPackageId, sourceId, dependencyName);
					}

					/*
					 * update any parents that might point here.
					 */
					mDb.updateParentId(clazz, sourceId);
					/*
					 * Create any necessary parent relationships.
					 */
					if (hasParent) mDb.addParentName(mPackageId, sourceId, parentName);
				}
			}
			return super.visitClass(classNode,
				nameQualification + "." + classNode.getSimpleName().toString());
		}
	}

	private class ExecutableVisitor extends ElementScanner6<Object,Trees> {
		private String mQualification = null;
		private DocDb mDb = null;
		private int mPackageId = -1;
		private Elements mUtils = null;

		public void setDocDb(DocDb db) {
			mDb = db;
		}

		public void setPackageId(int id) {
			mPackageId = id;
		}

		public void setUtils(Elements utils) {
			mUtils = utils;
		}

		private String getEnclosingElements(Element e) {
			Element encl = e.getEnclosingElement();
			if (encl == null) return "";
			else if (encl instanceof PackageElement) return "";
			else {
				System.err.println("Simple name: " + encl.getSimpleName().toString());
				return getEnclosingElements(encl) +
				"." +
				encl.getSimpleName().toString();
			}
		}

		private String fullExecutableName(ExecutableElement e) {
			String name = e.getSimpleName().toString();

			if (!name.contains(".")) {
				name = mUtils.getPackageOf(e) + getEnclosingElements(e) + "." + name;
			}
			return name;
		}

		private String className(ExecutableElement e) {
			return mUtils.getPackageOf(e) + "." + e.getEnclosingElement().getSimpleName();
		}

		public Object visitExecutable(ExecutableElement execElement, Trees trees) {
			if (mDb != null && mPackageId != -1) {
				int sourceId = -1;
				MethodTree execTree = null;
				BlockTree b = null;
				String body = null;
				String methodName = null;
				int parameterCount = 0;

				execTree = trees.getTree(execElement);
				b = execTree.getBody();
				parameterCount = execElement.getParameters().size();

				if (b == null) {
					System.err.println("Skipping empty method.");
					return super.visitExecutable(execElement, trees);
				}

				methodName = fullExecutableName(execElement);
				System.err.println("Method: " + methodName);

				sourceId = mDb.getSourceIdFromName(mPackageId, methodName, parameterCount);

				if (sourceId == -1) {
					/*
					 * Put in this source if we need to.
					 */
					sourceId = mDb.addSource(mPackageId, "method", "", methodName, parameterCount, "");
				}

				if (sourceId != -1) {
					body = new String();
					for (StatementTree statement : b.getStatements()) {
						body += statement.toString() + ";\n";
					}
					mDb.updateSource(mPackageId, sourceId, body);

					for (VariableElement v : execElement.getParameters()) {
						String parameterType = v.asType().toString();
						String parameterName = v.getSimpleName().toString();

						mDb.addParameter(mPackageId,sourceId,parameterType,parameterName);
					}
				} else {
					System.err.println("Warning: Could not update source for method " + methodName + ".");
				}
				/*
				System.out.println(body);
				*/
			}
			return super.visitExecutable(execElement, trees);
		}
	}
}
