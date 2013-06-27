package org.testmp.sync;

import org.testmp.sync.TestDoc;
import org.testng.annotations.Test;

public class TestSyncForTestNGTest {

    @Test(groups = { "P3", "sanity test" })
    @TestDoc(
        project = "TestNG project 1", 
        name = "TestNG feature 1", 
        description = "Description about the test", 
        groups = { "invalid" })
    public void testForTestNG1() {
        System.out.println("[TestSyncForTestNGTest#testForTestNG1]");
    }

}
