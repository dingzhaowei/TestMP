package org.testmp.sync;

import org.testng.annotations.Test;

public class TestSyncForTestNGTest {

    @Test(groups = { "P0" })
    @TestDoc(project = "项目 3", groups = { "sanity" }, description = "Sanity test of primary functions: create, update, delete")
    public void sanityTest() {
        System.out.println("[TestSyncForTestNGTest#sanityTest]");
    }

}
