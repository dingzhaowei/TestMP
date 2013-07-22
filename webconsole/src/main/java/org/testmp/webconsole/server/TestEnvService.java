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
import org.testmp.webconsole.model.Execution;
import org.testmp.webconsole.model.Host;
import org.testmp.webconsole.model.Task;
import org.testmp.webconsole.model.TestEnvironment;
import org.testmp.webconsole.util.CronExpression;

@SuppressWarnings("serial")
public class TestEnvService extends HttpServlet {

    private static Logger log = Logger.getLogger(TestEnvService.class);

    private DataStoreClient client;

    @Override
    public void init() throws ServletException {
        client = new DataStoreClient((String) getServletContext().getAttribute("testEnvStoreUrl"));
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
        String dsId = dsRequest.get("dataSource").toString();

        try {
            if (operationType.equals("fetch")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> criteria = (Map<String, Object>) dsRequest.get("data");
                List<Map<String, Object>> dataList = fetchData(criteria, dsId);
                responseBody.put("status", 0);
                responseBody.put("startRow", 0);
                responseBody.put("endRow", dataList.size());
                responseBody.put("totalRows", dataList.size());
                JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(dataList));
                responseBody.put("data", dataNode);
            } else if (operationType.equals("add")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) dsRequest.get("data");
                Map<String, Object> addedData = addData(data, dsId);
                responseBody.put("status", 0);
                JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(addedData));
                responseBody.put("data", dataNode);
            } else if (operationType.equals("remove")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) dsRequest.get("data");
                Map<String, Object> removedData = removeData(data, dsId);
                responseBody.put("status", 0);
                JsonNode dataNode = mapper.readTree(mapper.writeValueAsString(removedData));
                responseBody.put("data", dataNode);
            } else if (operationType.equals("update")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) dsRequest.get("data");
                @SuppressWarnings({ "unchecked" })
                Map<String, Object> oldValues = (Map<String, Object>) dsRequest.get("oldValues");
                Map<String, Object> updatedData = updateData(data, oldValues, dsId);
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

    private List<Map<String, Object>> fetchData(Map<String, Object> criteria, String dsId) throws Exception {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (dsId.equals("testEnvDS")) {
            List<DataInfo<TestEnvironment>> dataInfoList = client.getDataByTag(TestEnvironment.class, "TestEnv");
            for (DataInfo<TestEnvironment> dataInfo : dataInfoList) {
                TestEnvironment data = dataInfo.getData();
                Map<String, Object> m = data.toMap();
                m.put("envId", dataInfo.getId());
                result.add(m);
            }
            return result;
        } else if (dsId.equals("taskDS")) {
            List<DataInfo<Task>> dataInfoList = client.getData(Task.class, new String[] { "Task" }, criteria);
            for (DataInfo<Task> dataInfo : dataInfoList) {
                Task data = dataInfo.getData();
                Map<String, Object> m = data.toMap();
                m.put("taskId", dataInfo.getId());
                result.add(m);
            }
            return result;
        } else if (dsId.equals("executionDS")) {
            List<DataInfo<Execution>> dataInfoList = client.getData(Execution.class, new String[] { "Execution" },
                    criteria);
            for (DataInfo<Execution> dataInfo : dataInfoList) {
                Execution data = dataInfo.getData();
                Map<String, Object> m = data.toMap();
                m.put("executionId", dataInfo.getId());
                result.add(m);
            }
            return result;
        } else if (dsId.equals("hostDS")) {
            List<DataInfo<Host>> dataInfoList = client.getData(Host.class, new String[] { "Host" }, criteria);
            for (DataInfo<Host> dataInfo : dataInfoList) {
                Host data = dataInfo.getData();
                Map<String, Object> m = data.toMap();
                m.put("hostId", dataInfo.getId());
                result.add(m);
            }
            return result;
        }
        throw new RuntimeException("Unsupported datasource");
    }

    private Map<String, Object> addData(Map<String, Object> data, String dsId) throws Exception {
        if (dsId.equals("testEnvDS")) {
            checkEnvNameValidity(data);

            TestEnvironment testEnv = new TestEnvironment();
            testEnv.setEnvName((String) data.get("envName"));
            testEnv.setRefUrl((String) data.get("refUrl"));

            DataInfo<TestEnvironment> dataInfo = new DataInfo<TestEnvironment>(null,
                    Arrays.asList(new String[] { "TestEnv" }), testEnv);
            int id = client.addData(dataInfo).get(0);
            dataInfo = client.getDataById(TestEnvironment.class, id);
            TestEnvironment addedData = dataInfo.getData();
            Map<String, Object> m = addedData.toMap();
            m.put("envId", dataInfo.getId());
            return m;
        } else if (dsId.equals("taskDS")) {
            checkTaskScheduleValidity(data);

            Task task = new Task();
            task.setTaskName((String) data.get("taskName"));
            task.setEnvName((String) data.get("envName"));
            task.setStatus((String) data.get("status"));
            task.setSchedule((String) data.get("schedule"));
            task.setLastRunTime((String) data.get("lastRunTime"));

            DataInfo<Task> dataInfo = new DataInfo<Task>(null, Arrays.asList(new String[] { "Task" }), task);
            int id = client.addData(dataInfo).get(0);
            dataInfo = client.getDataById(Task.class, id);
            Task addedData = dataInfo.getData();
            Map<String, Object> m = addedData.toMap();
            m.put("taskId", dataInfo.getId());
            return m;
        } else if (dsId.equals("executionDS")) {
            Execution execution = new Execution();
            execution.setTaskName((String) data.get("taskName"));
            execution.setEnvName((String) data.get("envName"));
            execution.setSelected((Boolean) data.get("selected"));
            execution.setHost((String) data.get("host"));
            execution.setWorkingDir((String) data.get("workingDir"));
            execution.setCommand((String) data.get("command"));
            execution.setRetCode((Integer) data.get("retCode"));

            DataInfo<Execution> dataInfo = new DataInfo<Execution>(null, Arrays.asList(new String[] { "Execution" }),
                    execution);
            int id = client.addData(dataInfo).get(0);
            dataInfo = client.getDataById(Execution.class, id);
            Execution addedData = dataInfo.getData();
            Map<String, Object> m = addedData.toMap();
            m.put("executionId", dataInfo.getId());
            return m;
        } else if (dsId.equals("hostDS")) {
            checkHostNameValidity(data);

            Host host = new Host();
            host.setHostname((String) data.get("hostname"));
            host.setUsername((String) data.get("username"));
            host.setPassword((String) data.get("password"));

            DataInfo<Host> dataInfo = new DataInfo<Host>(null, Arrays.asList(new String[] { "Host" }), host);
            int id = client.addData(dataInfo).get(0);
            dataInfo = client.getDataById(Host.class, id);
            Host addedData = dataInfo.getData();
            Map<String, Object> m = addedData.toMap();
            m.put("hostId", dataInfo.getId());
            return m;
        }
        throw new RuntimeException("Unsupported datasource");
    }

    private Map<String, Object> removeData(Map<String, Object> data, String dsId) throws Exception {
        Map<String, Object> m = new HashMap<String, Object>();
        if (dsId.equals("testEnvDS")) {
            Integer envId = (Integer) data.get("envId");
            removeEnvironments(envId);
            m.put("envId", envId);
            return m;
        } else if (dsId.equals("taskDS")) {
            Integer taskId = (Integer) data.get("taskId");
            removeTasks(taskId);
            m.put("taskId", taskId);
            return m;
        } else if (dsId.equals("executionDS")) {
            Integer executionId = (Integer) data.get("executionId");
            removeExecutions(executionId);
            m.put("executionId", executionId);
            return m;
        } else if (dsId.equals("hostDS")) {
            Integer hostId = (Integer) data.get("hostId");
            if (!client.deleteData(hostId)) {
                throw new RuntimeException("Cannot remove the host");
            }
            m.put("hostId", hostId);
            return m;
        }
        throw new RuntimeException("Unsupported datasource");
    }

    private void removeExecutions(Integer... executionIds) throws Exception {
        if (!client.deleteData(executionIds)) {
            throw new RuntimeException("Cannot remove the executions");
        }
    }

    private void removeTasks(Integer... taskIds) throws Exception {
        for (Integer taskId : taskIds) {
            Task task = client.getDataById(Task.class, taskId).getData();
            String taskName = task.getTaskName();
            String envName = task.getEnvName();
            Map<String, Object> criteria = new HashMap<String, Object>();
            criteria.put("taskName", taskName);
            criteria.put("envName", envName);
            List<Integer> executionIds = client.findData(new String[] { "Execution" }, criteria);
            if (!executionIds.isEmpty()) {
                removeExecutions(executionIds.toArray(new Integer[0]));
            }
        }
        if (!client.deleteData(taskIds)) {
            throw new RuntimeException("Cannot remove the tasks");
        }
    }

    private void removeEnvironments(Integer... envIds) throws Exception {
        for (Integer envId : envIds) {
            TestEnvironment env = client.getDataById(TestEnvironment.class, envId).getData();
            String envName = env.getEnvName();
            Map<String, Object> criteria = new HashMap<String, Object>();
            criteria.put("envName", envName);
            List<Integer> taskIds = client.findData(new String[] { "Task" }, criteria);
            if (!taskIds.isEmpty()) {
                removeTasks(taskIds.toArray(new Integer[0]));
            }
        }
        if (!client.deleteData(envIds)) {
            throw new RuntimeException("Cannot remove the environments");
        }
    }

    private Map<String, Object> updateData(Map<String, Object> data, Map<String, Object> oldValues, String dsId)
            throws Exception {
        if (dsId.equals("testEnvDS")) {
            Integer envId = (Integer) data.get("envId");
            for (String key : data.keySet()) {
                if (key.equals("envId")) {
                    continue;
                }
                Object value = data.get(key);
                if (key.equals("envName")) {
                    checkEnvNameValidity(data);
                    Map<String, Object> criteria = new HashMap<String, Object>();
                    criteria.put("envName", (String) oldValues.get("envName"));
                    for (int id : client.findData(new String[] { "Execution" }, criteria)) {
                        client.addPropertyToData(id, "envName", value);
                    }
                    for (int id : client.findData(new String[] { "Task" }, criteria)) {
                        client.addPropertyToData(id, "envName", value);
                    }
                }
                client.addPropertyToData(envId, key, value);
            }
            DataInfo<TestEnvironment> dataInfo = client.getDataById(TestEnvironment.class, envId);
            TestEnvironment updatedData = dataInfo.getData();
            Map<String, Object> m = updatedData.toMap();
            m.put("envId", dataInfo.getId());
            return m;
        } else if (dsId.equals("taskDS")) {
            Integer taskId = (Integer) data.get("taskId");
            for (String key : data.keySet()) {
                if (key.equals("taskId")) {
                    continue;
                }
                Object value = data.get(key);
                if (key.equals("taskName")) {
                    Map<String, Object> criteria = new HashMap<String, Object>();
                    criteria.put("envName", (String) oldValues.get("envName"));
                    criteria.put("taskName", (String) oldValues.get("taskName"));
                    for (int id : client.findData(new String[] { "Execution" }, criteria)) {
                        client.addPropertyToData(id, "taskName", value);
                    }
                } else if (key.equals("schedule")) {
                    checkTaskScheduleValidity(data);
                }
                client.addPropertyToData(taskId, key, value);
            }
            DataInfo<Task> dataInfo = client.getDataById(Task.class, taskId);
            Task updatedData = dataInfo.getData();
            Map<String, Object> m = updatedData.toMap();
            m.put("taskId", dataInfo.getId());
            return m;
        } else if (dsId.equals("executionDS")) {
            Integer executionId = (Integer) data.get("executionId");
            for (String key : data.keySet()) {
                if (key.equals("executionId")) {
                    continue;
                }
                Object value = data.get(key);
                client.addPropertyToData(executionId, key, value);
            }
            DataInfo<Execution> dataInfo = client.getDataById(Execution.class, executionId);
            Execution updatedData = dataInfo.getData();
            Map<String, Object> m = updatedData.toMap();
            m.put("executionId", dataInfo.getId());
            return m;
        } else if (dsId.equals("hostDS")) {
            Integer hostId = (Integer) data.get("hostId");
            for (String key : data.keySet()) {
                if (key.equals("hostId")) {
                    continue;
                }
                Object value = data.get(key);
                if (key.equals("hostname")) {
                    checkHostNameValidity(data);
                }
                client.addPropertyToData(hostId, key, value);
            }
            DataInfo<Host> dataInfo = client.getDataById(Host.class, hostId);
            Host updatedData = dataInfo.getData();
            Map<String, Object> m = updatedData.toMap();
            m.put("hostId", dataInfo.getId());
            return m;
        }
        throw new RuntimeException("Unsupported datasource");
    }

    private void checkEnvNameValidity(Map<String, Object> data) throws Exception {
        if (data.get("envName") == null) {
            throw new RuntimeException("The environment name cannot be empty");
        }

        Map<String, Object> m = new HashMap<String, Object>();
        m.put("envName", data.get("envName"));
        if (!client.findData(new String[] { "TestEnv" }, m).isEmpty()) {
            throw new RuntimeException("There has been an environment with the same name");
        }
    }

    private void checkTaskScheduleValidity(Map<String, Object> data) throws Exception {
        if (data.get("schedule") == null) {
            return;
        }

        String cronExpression = data.get("schedule").toString();
        if (!CronExpression.isValidExpression(cronExpression)) {
            throw new RuntimeException("Invalid cron expression");
        }
    }

    private void checkHostNameValidity(Map<String, Object> data) throws Exception {
        if (data.get("hostname") == null) {
            throw new RuntimeException("The host name cannot be empty");
        }

        Map<String, Object> m = new HashMap<String, Object>();
        m.put("hostname", data.get("hostname"));
        if (!client.findData(new String[] { "Host" }, m).isEmpty()) {
            throw new RuntimeException("There has been a host with the same name");
        }
    }
}
