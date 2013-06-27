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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.runner.Description;
import org.junit.runner.Request;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.testmp.datastore.client.DataInfo;
import org.testmp.datastore.client.DataStoreClient;
import org.testmp.datastore.client.DataStoreClientException;
import org.testmp.sync.TestSync;
import org.testmp.sync.TestCase;
import org.testmp.sync.TestConfig;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class SpecificTests extends Runner {

    private Class<?> launcher;

    private List<String> includedAutomations = new LinkedList<String>();

    public SpecificTests(Class<?> launcher) throws InitializationError {
        try {
            this.launcher = launcher;
            String runTestConfigFile = TestConfig.getProperty("runtest.file");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(runTestConfigFile);
            doc.getDocumentElement().normalize();
            Set<TestCase> testCases = getTestCasesToRun(doc);
            for (TestCase testCase : testCases) {
                String automation = testCase.getAutomation();
                includedAutomations.add(automation);
            }
        } catch (Throwable e) {
            throw new InitializationError(e);
        }
    }

    @Override
    public Description getDescription() {
        Description description = Description.createSuiteDescription(launcher.getSimpleName());
        try {
            for (String automation : includedAutomations) {
                String[] classAndMethod = automation.split("#");
                Class<?> clazz = Class.forName(classAndMethod[0]);
                String name = classAndMethod[1];
                description.addChild(Description.createTestDescription(clazz, name));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return description;
    }

    @Override
    public void run(RunNotifier notifier) {
        try {
            Collections.sort(includedAutomations);
            for (String automation : includedAutomations) {
                String[] classAndMethod = automation.split("#");
                String className = classAndMethod[0];
                String methodName = classAndMethod[1];
                Class<?> clazz = Class.forName(className);
                Request request = Request.method(clazz, methodName);
                request.getRunner().run(notifier);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void populateIncludeCriteria(Element root, List<String> tags, Map<String, Object> properties) {
        for (String name : new String[] { "tag", "project", "name", "automation" }) {
            NodeList nodeList = root.getElementsByTagName(name);
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element elem = (Element) nodeList.item(i);
                String value = elem.getTextContent().trim();
                if (elem.getNodeName().equals("tag")) {
                    tags.add(value);
                } else {
                    properties.put(elem.getNodeName(), value);
                }
            }
        }
    }

    private void populateExcludeCriteria(Element root, Map<String, List<Object>> tagsAndProperties) {
        for (String name : new String[] { "tag", "project", "name", "automation" }) {
            NodeList nodeList = root.getElementsByTagName(name);
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element elem = (Element) nodeList.item(i);
                String key = elem.getNodeName();
                String value = elem.getTextContent().trim();
                if (!tagsAndProperties.containsKey(key)) {
                    tagsAndProperties.put(key, new ArrayList<Object>());
                }
                tagsAndProperties.get(key).add(value);
            }
        }
    }

    private Set<TestCase> getTestCasesToRun(Document doc) throws DataStoreClientException {
        Set<TestCase> testCasesToRun = new HashSet<TestCase>();
        NodeList includeNodes = doc.getElementsByTagName("include");
        NodeList excludeNodes = doc.getElementsByTagName("exclude");

        Map<String, List<Object>> excludedTagsAndProperties = new HashMap<String, List<Object>>();
        if (excludeNodes.getLength() > 2) {
            throw new RuntimeException("Only 1 exclude node is allowed");
        } else if (excludeNodes.getLength() > 0) {
            populateExcludeCriteria((Element) excludeNodes.item(0), excludedTagsAndProperties);
        }

        DataStoreClient client = TestSync.getTestCaseStoreClient();

        for (int i = 0; i < includeNodes.getLength(); i++) {
            Element includeNode = (Element) includeNodes.item(i);
            List<String> tags = new ArrayList<String>();
            Map<String, Object> properties = new HashMap<String, Object>();
            populateIncludeCriteria(includeNode, tags, properties);
            tags.add("TestCase");
            String[] tagArray = tags.toArray(new String[0]);
            List<DataInfo<TestCase>> dataInfoList = client.getData(TestCase.class, tagArray, properties);
            for (DataInfo<TestCase> dataInfo : dataInfoList) {
                TestCase testCase = dataInfo.getData();
                boolean excluded = false;
                for (String tag : dataInfo.getTags()) {
                    if (excludedTagsAndProperties.containsKey("tag")
                            && excludedTagsAndProperties.get("tag").contains(tag)) {
                        excluded = true;
                        break;
                    }
                }
                if (!excluded) {
                    excluded = (excludedTagsAndProperties.containsKey("project") && excludedTagsAndProperties.get(
                            "project").contains(testCase.getProject()))
                            || (excludedTagsAndProperties.containsKey("name") && excludedTagsAndProperties.get("name")
                                    .contains(testCase.getName()))
                            || (excludedTagsAndProperties.containsKey("automation") && excludedTagsAndProperties.get(
                                    "automation").contains(testCase.getAutomation()));
                }
                if (!excluded) {
                    testCasesToRun.add(testCase);
                }
            }
        }
        return testCasesToRun;
    }
}
