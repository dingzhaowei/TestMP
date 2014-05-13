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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
public class TestCaseService extends HttpServlet {

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
        BufferedReader input = new BufferedReader(new InputStreamReader(req.getInputStream(), "ISO-8859-1"));

        StringBuilder sb = new StringBuilder();
        while (true) {
            String line = input.readLine();
            if (line == null) {
                break;
            }
            sb.append(line).append('\n');
        }

        String requestBody = new String(sb.toString().getBytes("ISO-8859-1"), "UTF-8");
        log("Received POST request: " + requestBody);

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> dsRequest = mapper.readValue(requestBody, new TypeReference<Map<String, Object>>() {
        });

        ObjectNode dsResponse = mapper.createObjectNode();
        ObjectNode responseBody = dsResponse.putObject("response");
        String dataSource = dsRequest.get("dataSource").toString();
        String operationType = dsRequest.get("operationType").toString();
        Map<String, Object> data = (Map<String, Object>) dsRequest.get("data");

        try {
            if (operationType.equals("fetch")) {
                List<Map<String, Object>> dataList = null;
                if (dataSource.equals("testCaseDS")) {
                    // TODO: filter by userName
                    data.remove("userName");
                    Criteria criteria = Criteria.valueOf(mapper.writeValueAsString(data));
                    dataList = getTestCases(criteria);
                } else if (dataSource.equals("testProjectDS")) {
                    Criteria criteria = Criteria.valueOf(mapper.writeValueAsString(data));
                    dataList = getTestProjects(criteria);
                }
                responseBody.put("status", 0);
                responseBody.put("startRow", 0);
                responseBody.put("endRow", dataList.size());
                responseBody.put("totalRows", dataList.size());
                JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(dataList));
                responseBody.put("data", dataNode);
            } else if (operationType.equals("add")) {
                Map<String, Object> addedCase = addTestCase(data);
                responseBody.put("status", 0);
                JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(addedCase));
                responseBody.put("data", dataNode);
            } else if (operationType.equals("update")) {
                Map<String, Object> updatedCase = updateTestCase(data);
                responseBody.put("status", 0);
                JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(updatedCase));
                responseBody.put("data", dataNode);
            } else if (operationType.equals("remove")) {
                Map<String, Object> removedCase = removeTestCase(data);
                responseBody.put("status", 0);
                JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(removedCase));
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

    private List<Map<String, Object>> getTestProjects(Criteria criteria) throws Exception {
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
            tc.setProject(automation.substring(0, sec));
        }

        if (tc.getName() == null) {
            int sec = automation.lastIndexOf('.');
            tc.setName(automation.substring(sec + 1));
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
