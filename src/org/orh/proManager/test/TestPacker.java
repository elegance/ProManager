package org.orh.proManager.test;


import java.io.FileNotFoundException;
import java.io.IOException;

import org.dom4j.DocumentException;
import org.junit.Test;
import org.orh.proManager.model.Config;
import org.orh.proManager.service.Packer;

/**     
 * @文件名称: Test.java  
 * @类路径: org.orh.proManager.test  
 * @描述: TODO  
 * @作者：orh  
 * @时间：2013-6-5 下午6:00:20  
 * @版本：V1.0     
 */
public class TestPacker {
	
	@Test
	public void test_01() throws FileNotFoundException, IOException, DocumentException{
		Packer packer = new Packer();
		packer.textGo(Config.getTextTestInstance());
	}
	@Test
	public void test_02() throws FileNotFoundException, IOException, DocumentException{
		Packer packer = new Packer();
		packer.jsonGo(Config.getJsonTestInstance());
	}
}

