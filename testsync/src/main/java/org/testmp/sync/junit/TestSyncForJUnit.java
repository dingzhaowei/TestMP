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

package org.testmp.sync.junit;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.testmp.sync.TestSync;

public class TestSyncForJUnit extends TestSync {

    private String testMethodName;

    @Rule
    public TestWatcher watchman = new TestWatcher() {

        private long startTime;

        private boolean passed;

        private String failureTrace;

        @Override
        protected void failed(Throwable e, Description description) {
            passed = false;
            StringWriter cache = new StringWriter();
            e.printStackTrace(new PrintWriter(cache, true));
            failureTrace = cache.toString();
        }

        @Override
        protected void succeeded(Description description) {
            passed = true;
        }

        @Override
        protected void starting(Description description) {
            startTime = System.currentTimeMillis();
            testMethodName = description.getMethodName();
            updateTestDocument();
        }

        @Override
        protected void finished(Description description) {
            long duration = System.currentTimeMillis() - startTime;
            updateTestMeasures(duration, passed, failureTrace);
        }
    };

    @Override
    protected String getTestMethodName() {
        return testMethodName;
    }

    @Override
    protected Class<?> getTestClass() {
        return getClass();
    }

}
