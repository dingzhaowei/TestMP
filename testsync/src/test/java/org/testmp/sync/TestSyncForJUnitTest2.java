package org.testmp.sync;

import org.junit.Assert;
import org.junit.Test;
import org.testmp.sync.TestDoc;
import org.testmp.sync.junit.TestSyncForJUnit;

public class TestSyncForJUnitTest2 extends TestSyncForJUnit {

    @Test
    @TestDoc(
            project = "Customer Review", 
            name="Create review for instant video", 
            description = "Create a review for instant video and check it's created successfully", 
            groups = { "P0", "prod" })
    public void testCreateReviewForInstantVideo() throws Exception {
        System.out.println("[TestSyncForJUnitTest2#testCreateReviewForInstantVideo]");
        if (Math.random() > 0.5) {
            Assert.assertTrue("I'm failed", false);
        }
    }

    @Test
    @TestDoc(
            project = "Customer Review", 
            name = "Review on the checkout page",
            groups = { "P1", "devo" })
    public void testReviewOnCheckoutPage() throws Exception {
        System.out.println("[TestSyncForJUnitTest2#testReviewOnCheckoutPage]");
        if (Math.random() > 0.5) {
            Assert.assertTrue("I'm failed", false);
        }
    }

    @Test
    @TestDoc(project = "Checkout", groups = { "P0", "devo", "broken" })
    public void testChangeBookingAddress() throws Exception {
        System.out.println("[TestSyncForJUnitTest2#testChangeBookingAddress]");
        Assert.assertTrue("I'm failed", false);
    }

    @Test
    @TestDoc(project = "Checkout", groups = { "P0", "devo" })
    public void testCheckoutFlow() throws Exception {
        System.out.println("[TestSyncForJUnitTest2#testCheckoutFlow]");
        if (Math.random() > 0.5) {
            Assert.assertTrue("I'm failed", false);
        }
    }

    @Test
    @TestDoc(project = "Checkout", groups = { "P1", "prod" })
    public void testOutOfStockProduct() throws Exception {
        System.out.println("[TestSyncForJUnitTest2#testOutOfStockProduct]");
        if (Math.random() > 0.5) {
            Assert.assertTrue("I'm failed", false);
        }
    }

    @Test
    @TestDoc(project = "Checkout", groups = { "P2", "devo" })
    public void testNotificationEmail() throws Exception {
        System.out.println("[TestSyncForJUnitTest2#testNotificationEmail]");
        Assert.assertTrue("I'm failed", false);
    }
}
