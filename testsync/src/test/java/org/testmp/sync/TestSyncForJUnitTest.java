package org.testmp.sync;

import org.junit.Assert;
import org.junit.Test;
import org.testmp.sync.TestDoc;
import org.testmp.sync.junit.TestSyncForJUnit;

public class TestSyncForJUnitTest extends TestSyncForJUnit {

    @Test
    @TestDoc(
        project = "Example Project 1", 
        name = "Feature1", 
        description = "测试点，测试步骤，以及预期结果等", 
        groups = { "P0", "regression" })
    public void testFeature1() throws Exception {
        System.out.println("[TestSyncForJUnitTest#testFeature1]");
    }

    @Test
    @TestDoc(
        project = "Example Project 1", 
        name = "Feature2", 
        description = "文档标注也可以省略，将采用默认值：项目为类名，测试名称为方法名", 
        groups = { "P1" })
    public void testFeature2() throws Exception {
        System.out.println("[TestSyncForJUnitTest#testFeature2]");
        if (Math.random() > 0.7) {
            Assert.assertTrue("I'm failed", false);
        }
    }

    @Test
    @TestDoc(
        project = "Example Project 1", 
        name = "Feature3", 
        groups = { "P0" })
    public void testFeature3() throws Exception {
        System.out.println("[TestSyncForJUnitTest#testFeature3]");
        if (Math.random() > 0.3) {
            Assert.assertTrue("I'm failed", false);
        }
    }
}
