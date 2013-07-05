/*
 * TestMP (Test Management Platform)
 * Copyright 2013 and beyond, Zhaowei Ding.
 *
 * TestMP is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License (MIT).
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */

package org.testmp.sync.testng;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.testmp.sync.TestSync;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public final class TestSyncForTestNG extends TestSync implements ITestListener {

    private Class<?> testClass;

    private String testMethodName;

    @Override
    public void onTestStart(ITestResult result) {
        testClass = result.getTestClass().getRealClass();
        testMethodName = result.getMethod().getMethodName();
        updateTestDocument();
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        long duration = result.getEndMillis() - result.getStartMillis();
        updateTestMeasures(duration, true, null);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        long duration = result.getEndMillis() - result.getStartMillis();
        Throwable e = result.getThrowable();
        StringWriter cache = new StringWriter();
        e.printStackTrace(new PrintWriter(cache, true));
        String failureTrace = cache.toString();
        updateTestMeasures(duration, false, failureTrace);
    }

    @Override
    protected String getTestMethodName() {
        return testMethodName;
    }

    @Override
    protected Class<?> getTestClass() {
        return testClass;
    }

    @Override
    public void onFinish(ITestContext result) {
    }

    @Override
    public void onStart(ITestContext result) {
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
    }

    @Override
    public void onTestSkipped(ITestResult result) {
    }

}
