package org.testmp.sync;

import org.junit.Assert;
import org.junit.Test;
import org.testmp.sync.TestDoc;
import org.testmp.sync.junit.TestSyncForJUnit;

public class TestSyncForJUnitTest2 extends TestSyncForJUnit {

    @Test
    @TestDoc(project = "Example Project 1", groups = { "P1", "devo" })
    public void testFeatureException1() throws Exception {
        System.out.println("[TestSyncForJUnitTest2#testFeatureException1]");
        if (Math.random() > 0.5) {
            Assert.assertTrue("I'm failed", false);
        }
    }

    @Test
    @TestDoc(project = "Example Project 1", groups = { "P0", "devo" })
    public void testFeatureException2() throws Exception {
        System.out.println("[TestSyncForJUnitTest2#testFeatureException2]");
        if (Math.random() > 0.5) {
            Assert.assertTrue("I'm failed", false);
        }
    }

    @Test
    @TestDoc(project = "Example Project 2", groups = { "P2" })
    public void exampleProject2_FeatureTest1() throws Exception {
        System.out.println("[TestSyncForJUnitTest2#exampleProject2_FeatureTest1]");
        if (Math.random() > 0.5) {
            Assert.assertTrue("I'm failed", false);
        }
    }

    @Test
    @TestDoc(project = "Example Project 2", groups = { "P2" })
    public void exampleProject2_FeatureTest2() throws Exception {
        System.out.println("[TestSyncForJUnitTest2#exampleProject2_FeatureTest2]");
        if (Math.random() > 0.5) {
            Assert.assertTrue("I'm failed", false);
        }
    }

    @Test
    @TestDoc(project = "Example Project 2", groups = { "P2" })
    public void exampleProject2_FeatureTest3() throws Exception {
        System.out.println("[TestSyncForJUnitTest2#exampleProject2_FeatureTest3]");
        if (Math.random() > 0.5) {
            Assert.assertTrue("I'm failed", false);
        }
    }

    @Test
    @TestDoc(project = "Example Project 2", groups = { "P3" })
    public void exampleProject2_FeatureTest4() throws Exception {
        System.out.println("[TestSyncForJUnitTest2#exampleProject2_FeatureTest4]");
        Assert.assertTrue("I'm failed", false);
    }
}
