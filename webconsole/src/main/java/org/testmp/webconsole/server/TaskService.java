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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.testmp.datastore.client.DataInfo;
import org.testmp.datastore.client.DataStoreClient;

@SuppressWarnings("serial")
public class TaskService extends HttpServlet {

    private static Logger log = Logger.getLogger(TaskService.class);

    private DataStoreClient client;

    private TaskRunner taskRunner;

    @Override
    public void init() throws ServletException {
        client = new DataStoreClient((String) getServletContext().getAttribute("testEnvStoreUrl"));
        taskRunner = (TaskRunner) getServletContext().getAttribute("taskRunner");
        super.init();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
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

        HashMap<String, String> params = new HashMap<String, String>();
        for (String param : requestBody.split("&")) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2) {
                params.put(keyValue[0].trim(), URLDecoder.decode(keyValue[1], "UTF-8").trim());
            } else {
                params.put(keyValue[0].trim(), "");
            }
        }

        resp.setCharacterEncoding("UTF-8");
        PrintWriter output = resp.getWriter();
        String action = params.get("action");
        try {
            if (action.equals("run")) {
                final Integer taskId = Integer.valueOf(params.get("taskId"));
                runTask(taskId, client, taskRunner);
                output.flush();
            } else if (action.equals("cancel")) {
                Integer taskId = Integer.valueOf(params.get("taskId"));
                client.addPropertyToData(taskId, "status", "failure.png");

                DataInfo<Map> dataInfo = client.getDataById(Map.class, taskId);
                if (dataInfo == null) {
                    log.warn("Failed to cancel task " + taskId + ". Not found.");
                    return;
                }

                Map task = dataInfo.getData();
                Map<String, Object> execCriteria = new HashMap<String, Object>();
                Object envName = task.get("envName");
                Object taskName = task.get("taskName");
                execCriteria.put("envName", envName);
                execCriteria.put("taskName", taskName);
                List<DataInfo<Map>> execInfoList = client
                        .getData(Map.class, new String[] { "Execution" }, execCriteria);
                for (DataInfo<Map> execInfo : execInfoList) {
                    Map execution = execInfo.getData();
                    taskRunner.cancelExecution(execution);
                }
            } else if (action.equals("queryTaskStatus")) {
                StringBuilder respBuilder = new StringBuilder();
                for (String t : params.get("taskIds").split(",")) {
                    Integer taskId = Integer.parseInt(t.trim());

                    DataInfo<Map> dataInfo = client.getDataById(Map.class, taskId);
                    if (dataInfo == null) {
                        continue;
                    }

                    Object status = dataInfo.getData().get("status");
                    Object lastRunTime = dataInfo.getData().get("lastRunTime");
                    if (status != null) {
                        if (respBuilder.length() > 0) {
                            respBuilder.append(',');
                        }
                        respBuilder.append(taskId + "=" + status + ";" + lastRunTime);
                    }
                }
                output.print(respBuilder.toString());
                output.flush();
            } else if (action.equals("queryExecutionTrace")) {
                Integer executionId = Integer.valueOf(params.get("executionId"));

                DataInfo<Map> dataInfo = client.getDataById(Map.class, executionId);
                if (dataInfo == null) {
                    output.println("Execution is not found.");
                    output.flush();
                    return;
                }

                Map execution = dataInfo.getData();
                String host = (String) execution.get("host");
                String workingDir = (String) execution.get("workingDir");
                String command = (String) execution.get("command");
                String traceFileDir = (String) getServletContext().getAttribute("traceFileDir");
                String filename = TaskRunner.getTraceFileName(host, workingDir, command);
                File traceFile = new File(traceFileDir + File.separator + filename);

                if (!traceFile.exists()) {
                    output.println("No trace is found.");
                } else {
                    BufferedReader reader = new BufferedReader(new FileReader(traceFile));
                    try {
                        while (true) {
                            String line = reader.readLine();
                            if (line == null) {
                                break;
                            }
                            output.println(line);
                        }
                    } finally {
                        reader.close();
                    }
                }
                output.flush();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServletException(e);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    static void runTask(Integer taskId, DataStoreClient client, TaskRunner runner) throws Exception {
        Map task = null;

        // Check and change the task status
        synchronized (runner) {
            DataInfo<Map> dataInfo = client.getDataById(Map.class, taskId);
            if (dataInfo == null) {
                log.warn("Failed to run task " + taskId + ". Not found.");
                return;
            }
            task = dataInfo.getData();
            Object status = task.get("status");
            if (status != null && status.toString().startsWith("running")) {
                return;
            }
            client.addPropertyToData(taskId, "status", "running.gif");
            client.addPropertyToData(taskId, "lastRunTime", System.currentTimeMillis());
        }

        Map<String, Object> execCriteria = new HashMap<String, Object>();
        Object envName = task.get("envName");
        Object taskName = task.get("taskName");
        execCriteria.put("envName", envName);
        execCriteria.put("taskName", taskName);
        execCriteria.put("selected", true);

        List<DataInfo<Map>> execInfoList = client.getData(Map.class, new String[] { "Execution" }, execCriteria);
        Map<Integer, Future<Integer>> executionWait = new HashMap<Integer, Future<Integer>>();
        for (DataInfo<Map> dataInfo : execInfoList) {
            Map execution = dataInfo.getData();

            String hostname = (String) execution.get("host");
            if (!hostname.equals("localhost") && !hostname.equals("127.0.0.1")) {
                Map<String, Object> hostCriteria = new HashMap<String, Object>();
                hostCriteria.put("hostname", hostname);
                List<DataInfo<Map>> hostInfoList = client.getData(Map.class, new String[] { "Host" }, hostCriteria);
                Map host = hostInfoList.get(0).getData();
                execution.put("username", host.get("username"));
                execution.put("password", host.get("password"));
            }

            Integer executionId = dataInfo.getId();
            executionWait.put(executionId, runner.addExecution(execution));
        }

        boolean successful = true;
        for (Map.Entry<Integer, Future<Integer>> wait : executionWait.entrySet()) {
            Integer executionId = wait.getKey();
            Integer retCode = wait.getValue().get();
            client.addPropertyToData(executionId, "retCode", retCode);
            if (retCode != 0) {
                successful = false;
            }
        }
        client.addPropertyToData(taskId, "status", successful ? "success.png" : "failure.png");
    }
}
