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

package org.testmp.sync;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.testmp.datastore.client.DataInfo;
import org.testmp.datastore.client.DataStoreClient;
import org.testmp.sync.TestCase.RunRecord;
import org.testng.annotations.Test;

public abstract class TestSync {

    public static final String TAG_TEST_CASE = "TestCase";

    private static DataStoreClient client;

    static {
        String testCaseStoreUrl = TestConfig.getProperty("testCaseStoreUrl");
        if (testCaseStoreUrl == null) {
            throw new RuntimeException("testCaseStoreUrl is not configured");
        }
        client = new DataStoreClient(testCaseStoreUrl);
    }

    public static DataStoreClient getTestCaseStoreClient() {
        return client;
    }

    /**
     * Update the test case document in test case store
     */
    public void updateTestDocument() {
        String update = TestConfig.getProperty("updateTestDocument");
        if (update == null || !update.toLowerCase().equals("true")) {
            return;
        }

        try {
            Class<?> testClass = getTestClass();
            String testMethodName = getTestMethodName();
            Method testMethod = testClass.getMethod(testMethodName);
            TestDoc testDoc = testMethod.getAnnotation(TestDoc.class);
            Test testNgTest = testMethod.getAnnotation(Test.class);
            DataInfo<TestCase> dataInfo = convertTestDocToData(testDoc, testNgTest);

            TestCase testCase = dataInfo.getData();
            HashMap<String, Object> queryParams = new HashMap<String, Object>();
            queryParams.put("automation", testCase.getAutomation());
            List<DataInfo<TestCase>> result = client.getData(TestCase.class, new String[] { TAG_TEST_CASE },
                    queryParams);

            if (result.isEmpty()) {
                client.addData(dataInfo);
            } else {
                DataInfo<TestCase> existentDataInfo = result.get(0);
                TestCase existentTestCase = existentDataInfo.getData();
                int dataId = existentDataInfo.getId();

                // Update existent test case
                if (!existentTestCase.getProject().equals(testCase.getProject())) {
                    client.addPropertyToData(dataId, "project", testCase.getProject());
                }
                if (!existentTestCase.getName().equals(testCase.getName())) {
                    client.addPropertyToData(dataId, "name", testCase.getName());
                }
                if (!existentTestCase.getDescription().equals(testCase.getDescription())) {
                    client.addPropertyToData(dataId, "description", testCase.getDescription());
                }
                for (String tag : dataInfo.getTags()) {
                    if (!existentDataInfo.getTags().contains(tag)) {
                        client.addTagToData(dataId, tag);
                    }
                }
                for (String tag : existentDataInfo.getTags()) {
                    if (!dataInfo.getTags().contains(tag)) {
                        client.deleteTagFromData(dataId, tag);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Update the test metrics in test case store
     * 
     * @param duration
     * @param passed
     * @param failureTrace
     */
    public void updateTestMetrics(long duration, boolean passed, String failureTrace) {
        String update = TestConfig.getProperty("updateTestMetrics");
        if (update == null || !update.toLowerCase().equals("true")) {
            return;
        }

        RunRecord record = new RunRecord();
        record.setRecordTime(Calendar.getInstance().getTimeInMillis());
        record.setPassed(passed);
        record.setFailureTrace(failureTrace);
        record.setDuration(duration);
        TestCase testCase = null;
        try {
            Map<String, Object> query = new HashMap<String, Object>();
            query.put("automation", getTestAutomationName());
            List<Integer> dataIds = client.findDataByProperty(query);
            if (dataIds.isEmpty()) {
                return;
            }

            int testCaseId = dataIds.get(0);
            testCase = client.getDataById(TestCase.class, testCaseId).getData();

            double lastRobustness = testCase.evaluateRobustness();
            LinkedList<RunRecord> runHistory = testCase.getRunHistory();
            runHistory.addFirst(record);
            int runHistoryCapacity = Integer.parseInt(TestConfig.getProperty("runHistoryCapacity"));
            if (runHistory.size() > runHistoryCapacity) {
                runHistory.removeLast();
            }
            client.addPropertyToData(testCaseId, "runHistory", runHistory);

            double currentRobustness = testCase.evaluateRobustness();
            if (currentRobustness > lastRobustness) {
                client.addPropertyToData(testCaseId, "robustnessTrend", TestCase.QUALITY_STATUS_UPGRADING);
            } else if (currentRobustness < lastRobustness) {
                client.addPropertyToData(testCaseId, "robustnessTrend", TestCase.QUALITY_STATUS_DEGRADING);
            } else {
                double epsilon = 0.01 / runHistoryCapacity;
                if (Math.abs(currentRobustness - 1.0) < epsilon) {
                    client.addPropertyToData(testCaseId, "robustnessTrend", TestCase.QUALITY_STATUS_ALWAYSGOOD);
                } else {
                    client.addPropertyToData(testCaseId, "robustnessTrend", TestCase.QUALITY_STATUS_ALWAYSBAD);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the test method name
     * 
     * @return
     */
    protected abstract String getTestMethodName();

    /**
     * Get the test class
     * 
     * @return
     */
    protected abstract Class<?> getTestClass();

    private String getTestAutomationName() {
        return getTestClass().getName() + "#" + getTestMethodName();
    }

    private DataInfo<TestCase> convertTestDocToData(TestDoc testDoc, Test testNgTest) {
        DataInfo<TestCase> dataInfo = new DataInfo<TestCase>();
        TestCase tc = new TestCase();
        String className = getTestClass().getSimpleName();
        String methodName = getTestMethodName();

        if (testDoc == null) {
            dataInfo.setTags(Arrays.asList(new String[] { TAG_TEST_CASE }));
            tc.setProject(className);
            tc.setName(methodName);
            tc.setDescription("");
        } else {
            List<String> tags = new LinkedList<String>();
            tags.add(TAG_TEST_CASE);
            tags.addAll(Arrays.asList(testDoc.groups()));
            dataInfo.setTags(tags);

            String testProject = testDoc.project();
            tc.setProject(testProject.isEmpty() ? className : testProject);

            String testName = testDoc.name();
            tc.setName(testName.isEmpty() ? methodName : testName);
            tc.setDescription(testDoc.description());
        }

        if (testNgTest != null) {
            List<String> tags = dataInfo.getTags();
            for (String group : testNgTest.groups()) {
                if (!tags.contains(group)) {
                    tags.add(group);
                }
            }
            dataInfo.setTags(tags);
        }

        tc.setAutomation(getTestClass().getName() + "#" + methodName);
        dataInfo.setData(tc);
        return dataInfo;
    }

}
