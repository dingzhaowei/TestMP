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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

@SuppressWarnings("serial")
public class TestCaseService extends HttpServlet {

    private static Logger log = Logger.getLogger(TestCaseService.class);

    private DataStoreClient client;

    @Override
    public void init() throws ServletException {
        client = new DataStoreClient((String)getServletContext().getAttribute("testCaseStoreUrl"));
        super.init();
    }

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
        String oprationType = dsRequest.get("operationType").toString();
        try {
            if (oprationType.equals("fetch")) {
                List<Map<String, Object>> dataList = fetchData();
                responseBody.put("status", 0);
                responseBody.put("startRow", 0);
                responseBody.put("endRow", dataList.size());
                responseBody.put("totalRows", dataList.size());
                JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(dataList));
                responseBody.put("data", dataNode);
            } else if (oprationType.equals("update")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) dsRequest.get("data");
                Map<String, Object> updatedData = updateData(data);
                responseBody.put("status", 0);
                JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(updatedData));
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

    private List<Map<String, Object>> fetchData() throws Exception {

        List<DataInfo<TestCase>> dataInfoList = client.getDataByTag(TestCase.class, "TestCase");
        Map<Integer, MetaInfo> metaInfoLookingup = new HashMap<Integer, MetaInfo>();

        if (!dataInfoList.isEmpty()) {
            List<Integer> dataIdList = new ArrayList<Integer>();
            for (DataInfo<TestCase> dataInfo : dataInfoList) {
                dataIdList.add(dataInfo.getId());
            }

            List<MetaInfo> metaInfoList = client.getMetaInfo(dataIdList.toArray(new Integer[0]));
            for (MetaInfo metaInfo : metaInfoList) {
                metaInfoLookingup.put(metaInfo.getDataId(), metaInfo);
            }
        }

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (DataInfo<TestCase> dataInfo : dataInfoList) {
            MetaInfo metaInfo = metaInfoLookingup.get(dataInfo.getId());
            Map<String, Object> m = combineInfoToMap(dataInfo, metaInfo);
            result.add(m);
        }
        return result;
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
        Map<String, Object> updatedData = combineInfoToMap(dataInfo, metaInfo);
        return updatedData;
    }

    private Map<String, Object> combineInfoToMap(DataInfo<TestCase> dataInfo, MetaInfo metaInfo) throws Exception {
        TestCase tc = dataInfo.getData();
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("id", dataInfo.getId().toString());
        m.put("project", tc.getProject());
        m.put("name", tc.getName());
        m.put("description", tc.getDescription());
        m.put("automation", tc.getAutomation());
        m.put("robustness", String.format("%.3f", tc.evaluateRobustness()));
        m.put("robustnessTrend", tc.getRobustnessTrend() + ".png");
        m.put("avgTestTime", String.format("%.1f", tc.evaluateAverageTestTime()));
        m.put("timeVolatility", String.format("%.3f", tc.evaluateTimeVolatility()));

        List<String> tags = dataInfo.getTags();
        StringBuilder sb = new StringBuilder();
        for (String tag : tags) {
            if (tag.equals("TestCase")) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(tag);
        }
        m.put("tags", sb.toString());
        m.put("createTime", metaInfo.getMetaInfo().get("create_time"));
        m.put("lastModifyTime", metaInfo.getMetaInfo().get("last_modify_time"));

        List<RunRecord> runRecordList = tc.getRunHistory();
        ObjectMapper mapper = new ObjectMapper();
        m.put("runHistory", mapper.writeValueAsString(runRecordList));

        if (!runRecordList.isEmpty()) {
            RunRecord record = runRecordList.get(runRecordList.size() - 1);
            Date lastRunTime = new Date(record.getRecordTime());
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            m.put("lastRunTime", format.format(lastRunTime));
        }

        return m;
    }
}
