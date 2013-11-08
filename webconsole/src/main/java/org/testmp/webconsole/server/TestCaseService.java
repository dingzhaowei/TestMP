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
        String operationType = dsRequest.get("operationType").toString();
        try {
            if (operationType.equals("fetch")) {
                List<Map<String, Object>> dataList = loader.load("TestCase");
                Criteria criteria = Criteria.valueOf(mapper.writeValueAsString(dsRequest.get("data")));
                if (criteria != null) {
                    Filter filter = new Filter(criteria);
                    dataList = filter.doFilter(dataList);
                }
                responseBody.put("status", 0);
                responseBody.put("startRow", 0);
                responseBody.put("endRow", dataList.size());
                responseBody.put("totalRows", dataList.size());
                JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(dataList));
                responseBody.put("data", dataNode);
            } else if (operationType.equals("update")) {
                Map<String, Object> data = (Map<String, Object>) dsRequest.get("data");
                Map<String, Object> updatedData = updateData(data);
                responseBody.put("status", 0);
                JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(updatedData));
                responseBody.put("data", dataNode);
            } else if (operationType.equals("remove")) {
                Map<String, Object> data = (Map<String, Object>) dsRequest.get("data");
                Map<String, Object> removedData = removeData(data);
                responseBody.put("status", 0);
                JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(removedData));
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

    private Map<String, Object> updateData(Map<String, Object> data) throws Exception {
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

    private Map<String, Object> removeData(Map<String, Object> data) throws Exception {
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
