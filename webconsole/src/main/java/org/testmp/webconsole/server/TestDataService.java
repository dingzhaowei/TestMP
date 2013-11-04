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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.type.TypeReference;
import org.testmp.datastore.client.DataInfo;
import org.testmp.datastore.client.DataStoreClient;
import org.testmp.datastore.client.MetaInfo;

@SuppressWarnings("serial")
public class TestDataService extends HttpServlet {

    private static Logger log = Logger.getLogger(TestDataService.class);

    private DataStoreClient client;

    @Override
    public void init() throws ServletException {
        client = new DataStoreClient((String) getServletContext().getAttribute("testDataStoreUrl"));
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
        String operationType = dsRequest.get("operationType").toString();
        try {
            if (operationType.equals("fetch")) {
                List<Map<String, Object>> dataList = fetchData();
                responseBody.put("status", 0);
                responseBody.put("startRow", 0);
                responseBody.put("endRow", dataList.size());
                responseBody.put("totalRows", dataList.size());
                JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(dataList));
                responseBody.put("data", dataNode);
            } else if (operationType.equals("update")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) dsRequest.get("data");
                @SuppressWarnings({ "unchecked" })
                Map<String, Object> oldValues = (Map<String, Object>) dsRequest.get("oldValues");
                Map<String, Object> updatedData = updateData(data, oldValues);
                responseBody.put("status", 0);
                JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(updatedData));
                responseBody.put("data", dataNode);
            } else if (operationType.equals("add")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) dsRequest.get("data");
                Map<String, Object> addedData = addData(data);
                responseBody.put("status", 0);
                JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(addedData));
                responseBody.put("data", dataNode);
            } else if (operationType.equals("remove")) {
                @SuppressWarnings("unchecked")
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

    @SuppressWarnings("rawtypes")
    private List<Map<String, Object>> fetchData() throws Exception {

        List<DataInfo<Map>> dataInfoList = client.getDataByRange(Map.class, 0, Integer.MAX_VALUE);
        Map<Integer, MetaInfo> metaInfoLookingup = new HashMap<Integer, MetaInfo>();

        if (!dataInfoList.isEmpty()) {
            List<Integer> dataIdList = new ArrayList<Integer>();
            for (DataInfo<Map> dataInfo : dataInfoList) {
                dataIdList.add(dataInfo.getId());
            }

            List<MetaInfo> metaInfoList = client.getMetaInfo(dataIdList.toArray(new Integer[0]));
            for (MetaInfo metaInfo : metaInfoList) {
                metaInfoLookingup.put(metaInfo.getDataId(), metaInfo);
            }
        }

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (DataInfo<Map> dataInfo : dataInfoList) {
            MetaInfo metaInfo = metaInfoLookingup.get(dataInfo.getId());
            Map<String, Object> m = combineInfoToMap(dataInfo, metaInfo);
            result.add(m);
        }
        return result;
    }

    @SuppressWarnings("rawtypes")
    private Map<String, Object> updateData(Map<String, Object> data, Map<String, Object> oldValues) throws Exception {
        Integer id = (Integer) data.get("id");

        for (Object key : data.keySet()) {
            if (key.equals("tags")) {
                String[] oldTags = oldValues.get("tags").toString().split("\\s*,\\s*");
                Set<String> oldTagsSet = new HashSet<String>(Arrays.asList(oldTags));

                String[] newTags = data.get("tags").toString().split("\\s*,\\s*");
                Set<String> newTagsSet = new HashSet<String>(Arrays.asList(newTags));

                Set<String> tagsToDelete = new HashSet<String>(oldTagsSet);
                tagsToDelete.removeAll(newTagsSet);

                Set<String> tagsToAdd = new HashSet<String>(newTagsSet);
                tagsToAdd.removeAll(oldTagsSet);

                for (String tag : tagsToDelete) {
                    client.deleteTagFromData(id, tag);
                }

                for (String tag : tagsToAdd) {
                    client.addTagToData(id, tag);
                }
            } else if (key.equals("properties")) {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> oldProps = mapper.readValue(oldValues.get("properties").toString(),
                        new TypeReference<Map<String, Object>>() {
                        });
                Map<String, Object> newProps = mapper.readValue(data.get("properties").toString(),
                        new TypeReference<Map<String, Object>>() {
                        });

                Set<String> propsToDelete = new HashSet<String>(oldProps.keySet());
                propsToDelete.removeAll(newProps.keySet());

                for (String propName : propsToDelete) {
                    client.deletePropertyFromData(id, propName);
                }

                for (Map.Entry<String, Object> entry : newProps.entrySet()) {
                    String propName = entry.getKey();
                    Object propValue = entry.getValue();
                    client.addPropertyToData(id, propName, propValue);
                }
            }
        }

        DataInfo<Map> dataInfo = client.getDataById(Map.class, id);
        MetaInfo metaInfo = client.getMetaInfo(id).get(0);
        Map<String, Object> updatedData = combineInfoToMap(dataInfo, metaInfo);
        return updatedData;
    }

    @SuppressWarnings("rawtypes")
    private Map<String, Object> addData(Map<String, Object> data) throws Exception {
        String[] tags = data.get("tags").toString().split("\\s*,\\s*");
        String props = data.get("properties").toString();
        ObjectMapper mapper = new ObjectMapper();
        DataInfo<Object> dataInfoToAdd = new DataInfo<Object>();
        dataInfoToAdd.setTags(Arrays.asList(tags));
        dataInfoToAdd.setData(mapper.readValue(props, new TypeReference<Map<String, Object>>() {
        }));
        Integer id = client.addData(dataInfoToAdd).get(0);

        DataInfo<Map> dataInfo = client.getDataById(Map.class, id);
        MetaInfo metaInfo = client.getMetaInfo(id).get(0);
        Map<String, Object> addedData = combineInfoToMap(dataInfo, metaInfo);
        return addedData;
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

    @SuppressWarnings("rawtypes")
    private Map<String, Object> combineInfoToMap(DataInfo<Map> dataInfo, MetaInfo metaInfo) throws Exception {
        Map data = dataInfo.getData();
        Map<String, Object> m = new HashMap<String, Object>();
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

        m.put("id", dataInfo.getId().toString());
        List<String> tags = dataInfo.getTags();
        Collections.sort(tags);
        StringBuilder sb = new StringBuilder();
        for (String tag : tags) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(tag);
        }
        m.put("tags", sb.toString());
        m.put("properties", writer.writeValueAsString(data));
        m.put("createTime", metaInfo.getMetaInfo().get("create_time"));
        m.put("lastModifyTime", metaInfo.getMetaInfo().get("last_modify_time"));

        return m;
    }
}
