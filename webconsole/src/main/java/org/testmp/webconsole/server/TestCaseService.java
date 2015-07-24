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

package org.testmp.webconsole.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.type.TypeReference;
import org.testmp.datastore.client.DataInfo;
import org.testmp.datastore.client.DataStoreClient;
import org.testmp.datastore.client.MetaInfo;
import org.testmp.sync.TestCase;
import org.testmp.sync.TestCase.RunRecord;
import org.testmp.webconsole.server.Filter.Criteria;

@SuppressWarnings("serial")
public class TestCaseService extends ServiceBase {

    private static Logger log = Logger.getLogger(TestCaseService.class);

    private DataStoreClient client;

    private DataAssemblyStrategy strategy;

    private DataLoader<TestCase> loader;

    @Override
    public void init() throws ServletException {
        String testCaseStoreUrl = (String) getServletContext().getAttribute("testCaseStoreUrl");
        client = new DataStoreClient(testCaseStoreUrl);
        strategy = new TestCaseAssemblyStrategy();
        loader = new DataLoader<TestCase>(testCaseStoreUrl, TestCase.class, strategy);
        super.init();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, Object> dsRequest = getDataSourceRequest(req);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode dsResponse = mapper.createObjectNode();
        ObjectNode responseBody = dsResponse.putObject("response");
        String dataSource = dsRequest.get("dataSource").toString();
        String operationType = dsRequest.get("operationType").toString();
        Map<String, Object> data = (Map<String, Object>) dsRequest.get("data");

        try {
            JsonNode dataNode = null;
            if (operationType.equals("fetch")) {
                if (dataSource.equals("testCaseDS")) {
                    data.remove("sid"); // TODO: filter by userName
                    Criteria criteria = Criteria.valueOf(mapper.writeValueAsString(data));
                    List<Map<String, Object>> dataList = getTestCases(criteria);
                    dataNode = mapper.readTree(mapper.writeValueAsString(dataList));
                } else if (dataSource.equals("testProjectDS")) {
                    List<Map<String, Object>> dataList = getTestProjects();
                    dataNode = mapper.readTree(mapper.writeValueAsString(dataList));
                } else if (dataSource.equals("testResultDS")) {
                    String automation = (String) data.get("automation");
                    Map<String, Object> testResult = getTestResult(automation);
                    dataNode = mapper.readTree(mapper.writeValueAsString(testResult));
                } else if (dataSource.equals("testRunDS")) {
                    String userName = (String) data.get("sid");
                    String automations = (String) data.get("automation");
                    List<Map<String, Object>> dataList = getTestRuns(automations, userName);
                    dataNode = mapper.readTree(mapper.writeValueAsString(dataList));
                }
                responseBody.put("status", 0);
                responseBody.put("data", dataNode);
            } else if (operationType.equals("add")) {
                if (dataSource.equals("testCaseDS")) {
                    Map<String, Object> addedCase = addTestCase(data);
                    dataNode = mapper.readTree(mapper.writeValueAsString(addedCase));
                } else if (dataSource.endsWith("testRunDS")) {
                    String userName = (String) data.get("sid");
                    String automation = (String) data.get("automation");
                    Map<String, Object> addedRun = addTestRun(automation, userName);
                    dataNode = mapper.readTree(mapper.writeValueAsString(addedRun));
                }
                responseBody.put("status", 0);
                responseBody.put("data", dataNode);
            } else if (operationType.equals("update")) {
                if (dataSource.equals("testCaseDS")) {
                    Map<String, Object> updatedCase = updateTestCase(data);
                    dataNode = mapper.readTree(mapper.writeValueAsString(updatedCase));
                }
                responseBody.put("status", 0);
                responseBody.put("data", dataNode);
            } else if (operationType.equals("remove")) {
                if (dataSource.equals("testCaseDS")) {
                    Map<String, Object> removedCase = removeTestCase(data);
                    dataNode = mapper.readTree(mapper.writeValueAsString(removedCase));
                } else if (dataSource.endsWith("testRunDS")) {
                    String userName = (String) data.get("sid");
                    String automation = (String) data.get("automation");
                    Map<String, Object> removedRun = removeTestRun(automation, userName);
                    dataNode = mapper.readTree(mapper.writeValueAsString(removedRun));
                }
                responseBody.put("status", 0);
                responseBody.put("data", dataNode);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            responseBody.put("status", -1);
            responseBody.put("data", e.getMessage());
        }

        resp.setCharacterEncoding("UTF-8");
        PrintWriter output = resp.getWriter();
        output.print(dsResponse.toString());
        output.flush();
    }

    private List<Map<String, Object>> getTestProjects() throws Exception {
        List<String> projects = client.getPropertyValues("project", "TestCase");
        Collections.sort(projects);
        List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
        for (String project : projects) {
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("project", project);
            dataList.add(data);
        }
        return dataList;
    }

    private Map<String, Object> getTestResult(String automation) throws Exception {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("automation", automation);
        DataInfo<TestCase> dataInfo = client.getDataByProperty(TestCase.class, properties).get(0);
        MetaInfo metaInfo = client.getMetaInfo(dataInfo.getId()).get(0);
        TestCaseAssemblyStrategy as = new TestCaseAssemblyStrategy();
        return as.assemble(dataInfo, metaInfo);
    }

    private List<Map<String, Object>> getTestRuns(String automations, String userName) throws Exception {
        List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
        String automationServiceUrl = (String) getSetting("automationSettings", "automationServiceUrl", userName);
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(automationServiceUrl);
        try {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("automation", automations));
            params.add(new BasicNameValuePair("action", "query"));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
            httpPost.setEntity(entity);
            HttpResponse resp = httpClient.execute(httpPost);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String r = EntityUtils.toString(resp.getEntity(), "UTF-8");
                String[] a = automations.split(",");
                if (a.length != r.length()) {
                    throw new RuntimeException("Doesn't match. Automations: " + a.length + ", Results: " + r.length());
                }
                for (int i = 0; i < a.length; i++) {
                    Map<String, Object> m = new HashMap<String, Object>();
                    m.put("automation", a[i]);
                    m.put("isRunning", r.charAt(i) == '0' ? false : true);
                    dataList.add(m);
                }
            } else {
                throw new RuntimeException(resp.getStatusLine().getReasonPhrase());
            }
        } finally {
            httpPost.releaseConnection();
        }
        return dataList;
    }

    private Map<String, Object> addTestRun(String automation, String userName) throws Exception {
        String automationServiceUrl = (String) getSetting("automationSettings", "automationServiceUrl", userName);
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(automationServiceUrl);
        try {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("automation", automation));
            params.add(new BasicNameValuePair("action", "launch"));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
            httpPost.setEntity(entity);
            HttpResponse resp = httpClient.execute(httpPost);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String r = EntityUtils.toString(resp.getEntity(), "UTF-8");
                Map<String, Object> m = new HashMap<String, Object>();
                m.put("automation", automation);
                m.put("isRunning", r.equals("1"));
                return m;
            } else {
                throw new RuntimeException(resp.getStatusLine().getReasonPhrase());
            }
        } finally {
            httpPost.releaseConnection();
        }
    }

    private Map<String, Object> removeTestRun(String automation, String userName) throws Exception {
        String automationServiceUrl = (String) getSetting("automationSettings", "automationServiceUrl", userName);
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(automationServiceUrl);
        try {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("automation", automation));
            params.add(new BasicNameValuePair("action", "cancel"));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
            httpPost.setEntity(entity);
            HttpResponse resp = httpClient.execute(httpPost);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String r = EntityUtils.toString(resp.getEntity(), "UTF-8");
                Map<String, Object> m = new HashMap<String, Object>();
                m.put("automation", automation);
                m.put("isRunning", r.equals("1"));
                return m;
            } else {
                throw new RuntimeException(resp.getStatusLine().getReasonPhrase());
            }
        } finally {
            httpPost.releaseConnection();
        }
    }

    private List<Map<String, Object>> getTestCases(Criteria criteria) throws Exception {
        List<Map<String, Object>> dataList = loader.load("TestCase");
        if (criteria != null) {
            Filter filter = new Filter(criteria);
            dataList = filter.doFilter(dataList);
        }
        return dataList;
    }

    private Map<String, Object> addTestCase(Map<String, Object> data) throws Exception {
        String automation = data.get("automation").toString();
        HashMap<String, Object> queryParams = new HashMap<String, Object>();
        queryParams.put("automation", automation);
        if (client.findData(new String[] { "TestCase" }, queryParams).size() > 0) {
            throw new RuntimeException("Duplicate automation");
        }

        DataInfo<TestCase> dataInfo = new DataInfo<TestCase>();
        TestCase tc = new TestCase();
        tc.setAutomation(automation);
        tc.setProject((String) data.get("project"));
        tc.setName((String) data.get("name"));
        tc.setDescription((String) data.get("description"));

        if (tc.getProject() == null) {
            int sec = automation.lastIndexOf('.');
            if (sec == -1) {
                tc.setProject(automation);
            } else {
                tc.setProject(automation.substring(0, sec));
            }
        }

        if (tc.getName() == null) {
            int sec = automation.lastIndexOf('.');
            if (sec == -1) {
                tc.setName(automation);
            } else {
                tc.setName(automation.substring(sec + 1));
            }
        }

        dataInfo.setData(tc);

        ArrayList<String> tags = new ArrayList<String>();
        tags.add("TestCase");
        if (data.get("tags") != null) {
            for (String t : data.get("tags").toString().trim().split(",")) {
                tags.add(t.trim());
            }
        }
        dataInfo.setTags(tags);

        Integer id = client.addData(dataInfo).get(0);
        dataInfo = client.getDataById(TestCase.class, id);
        MetaInfo metaInfo = client.getMetaInfo(id).get(0);
        Map<String, Object> addedData = strategy.assemble(dataInfo, metaInfo);
        return addedData;
    }

    private Map<String, Object> updateTestCase(Map<String, Object> data) throws Exception {
        Integer id = (Integer) data.get("id");
        TestCase tc = client.getDataById(TestCase.class, id).getData();

        // update run history
        ObjectMapper mapper = new ObjectMapper();
        List<RunRecord> runHistory = mapper.readValue(data.get("runHistory").toString(),
                new TypeReference<List<RunRecord>>() {
                });
        if (!tc.getRunHistory().equals(runHistory)) {
            client.addPropertyToData(id, "runHistory", runHistory);
        }

        DataInfo<TestCase> dataInfo = client.getDataById(TestCase.class, id);
        MetaInfo metaInfo = client.getMetaInfo(id).get(0);
        Map<String, Object> updatedData = strategy.assemble(dataInfo, metaInfo);
        return updatedData;
    }

    private Map<String, Object> removeTestCase(Map<String, Object> data) throws Exception {
        Integer id = (Integer) data.get("id");
        if (client.deleteData(id)) {
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("id", id);
            return m;
        } else {
            throw new RuntimeException("Cannot remove the data");
        }
    }
}
