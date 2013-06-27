package org.testmp.sync;

import org.testmp.sync.TestDoc;
import org.testng.annotations.Test;

public class TestSyncForTestNGTest {

    @Test(groups = { "P0", "sanity test" })
    @TestDoc(
        project = "TestMP", 
        name = "Test Case Management", 
        description = "Description about the test")
    public void sanityTest() {
        System.out.println("[TestSyncForTestNGTest#sanityTest]");
    }

}
