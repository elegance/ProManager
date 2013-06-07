package org.orh.proManager.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 * @文件名称: Config.java
 * @类路径: proManager
 * @描述: TODO 打包配置类
 * @作者：orh
 * @时间：2013-5-28 上午11:04:00
 * @版本：V1.0
 */
public class Config {

	private String recordFilePath; // 记录文件
	private ERecordType recordType; // 记录文件的格式

	private String projectPath; // 项目所在路径
	private String destPath; // 目标路径，打包输出路径
	private boolean isOutPathAppendDate = true; // 输出目录是否 ，默认加上日期文件夹

	// {{{ 根据 projectPath 来对下列 属性赋值，配置这些属性是因为 这些目录每个项目可能都不相同

	private String projectName; // 项目名称
	private String webRootName; // web根目录名称

	private String classpathFilePath; // .classpath 文件，eclipse:项目下的.classpath文件
										// ，附1 看.classpath的简要说明
	private String classOuputPath; // 类生成目录
	private List<String> srcListPath; // source 文件
	private List<String> libListFile; // lib jar文件所在

	// }}}

	/**
	 * 
	 * @param recordFilePath
	 *            根据标准定义的记录文件
	 * @param projectPath
	 *            项目路径
	 * @param destPath
	 *            输出包路径
	 * @throws FileNotFoundException
	 * @throws DocumentException
	 */
	public Config(String recordFilePath, ERecordType recordType,
			String projectPath, String destPath) throws FileNotFoundException,
			DocumentException {
		if (!pathIsExists(recordFilePath)) { // 记录文件不存在
			throw new FileNotFoundException(recordFilePath);
		}
		if (!pathIsExists(projectPath)) { // 项目路径存在
			throw new FileNotFoundException(projectPath);
		}
		if (!pathIsExists(projectPath + "/" + ".classpath")) {
			throw new FileNotFoundException(projectPath + "/.classpath");
		}
		this.classpathFilePath = projectPath + "/.classpath";
		this.recordFilePath = recordFilePath;
		this.recordType = recordType;
		this.projectPath = projectPath;
		this.destPath = destPath
				+ (isOutPathAppendDate ? ("/" + new SimpleDateFormat(
						"yyyy-MM-dd").format(new Date())) : "");
		// 解析 .classpath xml文件，赋值config中的一些变量
		analysisClassPathFile(this.classpathFilePath);
	}

	/**
	 * 
	 * @return 测试 config
	 * @throws FileNotFoundException
	 * @throws DocumentException
	 */
	public static Config getJsonTestInstance() throws FileNotFoundException,
			DocumentException {
		// String recordFilePath = "E:/aresoft/光大/直销机构报表-new/simpleTest.json";
		String recordFilePath = "E:/aresoft/光大/直销机构报表-new/修改记录_todo.json";
		ERecordType recordType = ERecordType.JSON_TYPE;
		String projectPath = "E:/workspace/10_workspace/gd_callcenter";
		String destPath = "E:/aresoft/光大/升级包/test";
		return new Config(recordFilePath, recordType, projectPath, destPath);
	}

	public static Config getTextTestInstance() throws FileNotFoundException,
			DocumentException {
		// String recordFilePath = "E:/aresoft/光大/直销机构报表-new/simpleTest.json";
		String recordFilePath = "E:/aresoft/光大/直销机构报表-new/TEST.txt";
		ERecordType recordType = ERecordType.TEXT_TYPE;
		String projectPath = "E:/workspace/10_workspace/gd_callcenter";
		String destPath = "E:/aresoft/光大/升级包/test";
		return new Config(recordFilePath, recordType, projectPath, destPath);
	}

	/**
	 * 分析 .classpath 文件，赋值config变量
	 * 
	 * @param classpathFilePath
	 *            "。classpath" 文件路径
	 * @throws DocumentException
	 */
	private void analysisClassPathFile(String classpathFilePath)
			throws DocumentException {
		File cfile = new File(classpathFilePath);
		this.projectName = cfile.getParentFile().getName();

		SAXReader reader = new SAXReader();
		Document doc = reader.read(cfile);
		Element rootElement = doc.getRootElement();
		@SuppressWarnings("unchecked")
		List<Element> elList = rootElement.elements();

		srcListPath = new ArrayList<String>();
		libListFile = new ArrayList<String>();

		for (Element e : elList) {
			String tmpKind = e.attributeValue("kind");
			String tmpPath = e.attributeValue("path");
			if ("src".equals(tmpKind)) {
				srcListPath.add(tmpPath);
			} else if ("output".equals(tmpKind)) {
				this.classOuputPath = tmpPath;
				this.webRootName = classOuputPath.substring(0,
						classOuputPath.indexOf("/"));
			} else if ("lib".equals(tmpKind)) {
				libListFile.add(tmpPath);
			}

		}
	}

	/**
	 * 判断文件或目录是否存在
	 * 
	 * @param path
	 *            文件或目录的路径
	 * @return true 存在 、false 不存在
	 */
	private boolean pathIsExists(String path) {
		File file = new File(path);
		if (file.exists()) {
			return true;
		}
		return false;
	}

	public String getRecordFilePath() {
		return recordFilePath;
	}

	public String getProjectPath() {
		return projectPath;
	}

	public String getClasspathFilePath() {
		return classpathFilePath;
	}

	public String getDestPath() {
		return destPath;
	}

	public String getclassOuputPath() {
		return classOuputPath;
	}

	public List<String> getSrcListPath() {
		return srcListPath;
	}

	public List<String> getLibListFile() {
		return libListFile;
	}

	public String getProjectName() {
		return projectName;
	}

	public String getWebRootName() {
		return webRootName;
	}

	public ERecordType getRecordType() {
		return recordType;
	}

	/**
	 * <classpathentry kind="src" path="src"/> #-> src 多个，即 source目录有多个，这些目录打包时
	 * 是放在 【classes】 下的一级目录的<br/>
	 * <classpathentry excluding="resources/" kind="src" path="conf"/> <br/>
	 * <classpathentry kind="src" path="conf/resources"/> <br/>
	 * <classpathentry kind="lib" path="WEB-INF/lib/xxxx.jar"/> #-> 一般存放jar包的位置<br/>
	 * <classpathentry kind="lib" path="WEB-INF/lib/xxxx.jar"/> <br/>
	 * <classpathentry kind="output" path="WEB-INF/classes"/> #-> class 输出的目录<br/>
	 * 
	 */

}
