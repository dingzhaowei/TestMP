package org.testmp.sync;

import org.junit.Assert;
import org.junit.Test;
import org.testmp.sync.junit.TestSyncForJUnit;

public class TestSyncForJUnitTest extends TestSyncForJUnit {

    @Test
    @TestDoc(
        project = "Customer Review", 
        name = "Top 100 reviewer", 
        description = "Open the top reivewer page and check the badge of the 100 first", 
        groups = { "P0", "prod" })
    public void testTop100Reviewers() throws Exception {
        System.out.println("[TestSyncForJUnitTest#testTop100Reviewers]");
    }

    @Test
    @TestDoc(
        project = "Customer Review", 
        name = "Create review for book", 
        description = "Create a review for book and check it's created successfully", 
        groups = { "P0", "prod", "broken" })
    public void testCreateReviewForBook() throws Exception {
        System.out.println("[TestSyncForJUnitTest#testCreateReviewForBook]");
        Assert.assertTrue("I'm failed", false);
    }

    @Test
    @TestDoc(
        project = "Customer Review", 
        name = "Unallowed reivewer", 
        description = "Create review as a customer without buying history, and check redirecting to the warning page",
        groups = { "P1", "prod" })
    public void testUnallowedReviewer() throws Exception {
        System.out.println("[TestSyncForJUnitTest#testUnallowedReviewer]");
        if (Math.random() > 0.5) {
            Assert.assertTrue("I'm failed", false);
        }
    }
}
