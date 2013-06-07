package org.orh.proManager.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.dom4j.DocumentException;
import org.junit.Test;
import org.orh.proManager.model.Config;
import org.orh.proManager.model.ERecordType;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * @文件名称: Packer.java
 * @类路径: proManager
 * @描述: TODO 根据todo json格式打升级包
 * @作者：orh
 * @时间：2013-5-27 下午1:54:22
 * @版本：V1.0
 */
public class Packer {

	private Config config; // 配置信息
	private List<String> textLineList;
	private String jsonStr;

	private final String NOT_RESOURCE_PREFIX = ".NOT_RESOURCE==> "; // 非资源文件 前缀
	private HashMap<String, String> cacheMap; // 缓存 "项目中的文件路径"—— "文件将输出的路径"
	private List<FileEntry> cpfileList = null;

	/**
	 * 初始化
	 * 
	 * @param config
	 */
	private void init(Config config) {
		this.config = config;
		cpfileList = new ArrayList<FileEntry>();
		cacheMap = new HashMap<String, String>();
		if (config.getRecordType().equals(ERecordType.JSON_TYPE)) {
			loadFile(new File(config.getRecordFilePath()),
					ERecordType.JSON_TYPE);
		} else if (config.getRecordType().equals(ERecordType.TEXT_TYPE)) {
			textLineList = new ArrayList<String>();
			loadFile(new File(config.getRecordFilePath()),
					ERecordType.TEXT_TYPE);
		}
	}

	/**
	 * 纯文本文件
	 * 
	 * @param config
	 * @throws IOException
	 */
	public void textGo(Config config) throws IOException {
		init(config);
		analysistextLineList();
		copyFile();
	}

	private void analysistextLineList() throws IOException {
		for (String line : textLineList) {
			filesGenerate(line);
		}
	}

	/**
	 * json 格式文件
	 * 
	 * @param config
	 * @throws IOException
	 * @throws DocumentException
	 */
	public void jsonGo(Config config) throws IOException, DocumentException {
		init(config);
		analysisJsonStr();
		copyFile();
	}

	private void analysisJsonStr() throws IOException {
		// 获取 json记录文件
		JSONObject rootJSON = JSON.parseObject(jsonStr);

		// 获取 各模块数组
		JSONArray modJSONArr = rootJSON.getJSONArray("modules");
		// 遍历各模块
		for (int i = 0; i < modJSONArr.size(); i++) {
			// 获取模块
			JSONObject modJSON = modJSONArr.getJSONObject(i);

			// 模块完成状态
			String status = modJSON.getString("status");
			// 模块标题
			String title = modJSON.getString("title");
			JSONObject detailJSON = modJSON.getJSONObject("detail");
			// System.out.printf("title: %s  status: %s \n", title, status);
			if (detailJSON != null) {
				// 模块修改的文件集合,files
				JSONArray filesJSONArr = detailJSON.getJSONArray("files");
				// this.filesGenerate(filesJSONArr);
				for (int j = 0; filesJSONArr !=null && j < filesJSONArr.size(); j++) {
					// file
					JSONObject fileJSON = filesJSONArr.getJSONObject(j);
					String desc = fileJSON.getString("desc");
					String filePath = fileJSON.getString("file");
					filesGenerate(filePath);
				}
				// 模块修改的sql集合, sqls
				JSONArray sqlsJSONArr = detailJSON.getJSONArray("sqls");
				this.sqlsGenerate(sqlsJSONArr);
			}

		}
	}

	/**
	 * 开始复制 list中的文件
	 * 
	 * @throws IOException
	 */
	private void copyFile() throws IOException {
		for (FileEntry f : cpfileList) {
			FileUtils.copyFile(f.src, f.target);
		}
	}

	/**
	 * 应用 升级文件的复制(包括：source、bin)
	 * 
	 * @param filesJSONArr
	 * @throws IOException
	 */
	private void filesGenerate(String filePath) throws IOException {
		addCpFileList(filePath);
	}

	/**
	 * 
	 * @param filePath
	 *            将文件添加值 要复制的 文件列表list
	 */
	private void addCpFileList(String filePath) {
		String noProNameFilePath = filePath.replaceFirst(
				"/" + config.getProjectName() + "/", ""); // 不包含项目名称的文件名
		String noFileNamePath = parentDirPath(noProNameFilePath); // 进一步 不包含文件名
		String fileName = filePath.substring(filePath.lastIndexOf("/") + 1); // 文件名
		String calcPath = "";

		// 根据 文件路径 判断得到 输出的路径（ 是否受 资源文件影响(主要：就是根据 .classpath文件去判断)）
		if (!cacheMap.containsKey(noFileNamePath)) {
			if (config.getSrcListPath().contains(noFileNamePath)) {
				// 直接在classpath下
				calcPath = "";
			} else {
				// 非直接就能判断是否受 资源文件影响的文件，需递归判断
				calcPath = notDirectly(noFileNamePath, null,
						config.getSrcListPath());
			}
			calcPath = calcPath.startsWith("/") ? calcPath
					.replaceFirst("/", "") : calcPath; // 去除前面的 “/”
			cacheMap.put(noFileNamePath, calcPath);
		} else {
			calcPath = cacheMap.get(noFileNamePath);
		}

		// 根据是否受资源文件影响条件，"拼接  输入、输出路径"
		if (!calcPath.startsWith(NOT_RESOURCE_PREFIX)) { // source folder
			if (fileName.endsWith(".java")) { // 取 源目录 java 备份，输出 目录 取
				// java文件
				String srcJava = config.getProjectPath() + "/"
						+ noProNameFilePath;
				String targetJava = config.getDestPath() + "/"
						+ config.getProjectName() + "/src/" + noFileNamePath
						+ "/" + fileName;
				cpfileList.add(new FileEntry(srcJava, targetJava));
				// 取class
				String noSuiffixFileName = fileName.substring(0,
						fileName.lastIndexOf("."));
				File[] classFiles = new File(config.getProjectPath() + "/"
						+ config.getclassOuputPath() + "/" + calcPath + "/")
						.listFiles();
				// 考虑内部类的情况
				String regex = String.format("^%s(\\$.*)?\\.class$",
						noSuiffixFileName);
				Pattern pattern = Pattern.compile(regex);
				for (File f : classFiles) {
					if (f.getName().startsWith(noSuiffixFileName)
							&& pattern.matcher(f.getName()).matches()) {
						File tmp = new File(config.getDestPath() + "/"
								+ config.getProjectName() + "/bin/"
								+ config.getclassOuputPath() + "/" + calcPath
								+ "/" + f.getName());
						cpfileList.add(new FileEntry(f, tmp));
					}
				}

			} else {

				String src = config.getProjectPath() + "/" + noProNameFilePath;
				String target = config.getDestPath() + "/"
						+ config.getProjectName() + "/bin/"
						+ config.getclassOuputPath() + calcPath + "/"
						+ fileName;
				String bakTarget = config.getDestPath() + "/"
						+ config.getProjectName() + "/src/" + noFileNamePath
						+ "/" + fileName;

				cpfileList.add(new FileEntry(src, target));
				cpfileList.add(new FileEntry(src, bakTarget));
			}

		} else {
			calcPath = calcPath.replaceFirst(NOT_RESOURCE_PREFIX, "");
			String src = config.getProjectPath() + "/" + noProNameFilePath;
			String target = config.getDestPath() + "/"
					+ config.getProjectName() + "/bin/" + calcPath + "/"
					+ fileName;
			cpfileList.add(new FileEntry(src, target));
		}

	}

	/**
	 * 
	 * 非直接就能判断是否 是资源文件的 目录层级 递归查找
	 * 
	 * @param allPath
	 * @param curPath
	 * @param srcList
	 *            <pre>
	 *            String allPath = aa/bb/cc/; String curPath = ""; 
	 *            List<String> srcList = {"aa","aa/bb/cc"}; 结果输出应该是 /bb/
	 * </pre>
	 * @return
	 */
	private String notDirectly(String allPath, String curPath,
			List<String> srcList) {
		List<String> matchsList = null;
		if (!allPath.equals(curPath)) {
			curPath = nextLevelPath(allPath, curPath);
		} else {
			return parentDirName(curPath);
		}
		for (String s : srcList) {
			if (matchsList == null) {
				matchsList = new ArrayList<String>();
			}
			if (s.startsWith(curPath)) {
				matchsList.add(s);
			}
		}

		if (matchsList.size() > 1) {
			// 递归下一级目录、判断
			return notDirectly(allPath, curPath, matchsList);
		}
		if (matchsList.size() == 1) {
			return allPath.replaceFirst(matchsList.get(0), "");
		}
		return NOT_RESOURCE_PREFIX + allPath; // 直接返回，即非资源文件
	}

	/**
	 * <p>
	 * 获取下一层目录的path
	 * </p>
	 * 
	 * @param allPath
	 *            全路径
	 * @param curPath
	 *            当前路径
	 * 
	 *            <pre>
	 * String allPath = &quot;aa/bb/cc/dd&quot;;
	 * String curPath = &quot;aa/bb&quot;;
	 * nextLevelPath(allPath, curPath); // ===&gt; aa/bb/cc
	 * </pre>
	 */
	private String nextLevelPath(String allPath, String curPath) {
		curPath = (curPath == null || curPath.equals("")) ? "" : curPath + "/";
		allPath = allPath.replaceFirst(curPath, "");
		allPath = (allPath != null && allPath.indexOf("/") < 0) ? allPath + "/"
				: allPath;
		return curPath + allPath.substring(0, allPath.indexOf("/"));
	}

	/**
	 * <p>
	 * 获取父级目录路径
	 * <p>
	 * 
	 * @param fileName
	 *            文件或目录路径
	 * 
	 *            <pre>
	 * String fileName = &quot;aa/bb/cc&quot;;
	 * parentDirPath(fileName); // ========&gt; aa/bb
	 * </pre>
	 */
	private String parentDirPath(String fileName) {
		return fileName.contains("/") ? fileName.substring(0,
				fileName.lastIndexOf("/")) : "";
	}

	/**
	 * <p>
	 * 获取父级目录名称
	 * <p>
	 * 
	 * @param fileName
	 *            文件或目录路径
	 * 
	 *            <pre>
	 * String fileName = &quot;aa/bb/cc&quot;;
	 * parentDirPath(fileName); // ========&gt; bb
	 * </pre>
	 */
	private String parentDirName(String fileName) {
		return fileName != null && fileName.contains("/") ? fileName
				.substring(fileName.lastIndexOf("/")) : "";
	}

	/**
	 * sql 升级 文件的生成
	 * 
	 * @param sqlsJSONArr
	 */
	private void sqlsGenerate(JSONArray sqlsJSONArr) {
		if (sqlsJSONArr != null) {
			for (int j = 0; j < sqlsJSONArr.size(); j++) {
				JSONObject sqlJSON = sqlsJSONArr.getJSONObject(j);
				String desc = sqlJSON.getString("desc");
				String sql = sqlJSON.getString("sql");
				// System.out.printf("desc: %s  sql: %s \n", desc, sql);
			}
		}
	}

	/**
	 * 加载json文件
	 * 
	 * @param file
	 *            json格式文件
	 */
	private void loadFile(File file, ERecordType filelType) {
		BufferedReader bf = null;
		StringBuffer strbuf = null;
		try {
			bf = new BufferedReader(new InputStreamReader(new FileInputStream(
					file), "GBK"));
			strbuf = new StringBuffer();
			String line = null;
			while (true) {
				line = bf.readLine();
				if (line == null) {
					jsonStr = strbuf.toString();
					break;
				}
				if (filelType.equals(ERecordType.JSON_TYPE)) {
					strbuf.append(line);
				} else if (filelType.equals(ERecordType.TEXT_TYPE)) {
					textLineList.add(line);
				}

			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				bf.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}
}

class FileEntry {
	@SuppressWarnings("unused")
	private FileEntry() {
	}

	public FileEntry(String srcPath, String targetPath) {
		this.src = new File(srcPath);
		this.target = new File(targetPath);
	}

	public FileEntry(File src, File target) {
		this.src = src;
		this.target = target;
	}

	File src;
	File target;
}