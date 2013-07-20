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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Timer;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

public class WebConsoleContextListener implements ServletContextListener {

    private static Logger log = Logger.getLogger(WebConsoleContextListener.class);

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        Properties config = new Properties();
        try {
            String settingPath = System.getProperty("user.home") + File.separator + ".testmp";

            String home = System.getenv("TESTMP_HOME");
            if (home != null && !home.isEmpty()) {
                settingPath = home + File.separator + "conf";
            }

            FileInputStream input = new FileInputStream(settingPath + File.separator + "testmp.properties");
            config.load(new InputStreamReader(input, "UTF-8"));
            for (Object key : config.keySet()) {
                String value = config.getProperty(key.toString()).toString();
                context.setAttribute(key.toString(), value);
                log.info("TestMP settings: " + key.toString() + "=" + value);
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }

        @SuppressWarnings("rawtypes")
        Enumeration initParamNames = context.getInitParameterNames();
        while (initParamNames.hasMoreElements()) {
            String key = initParamNames.nextElement().toString();
            if (context.getAttribute(key) == null) {
                String value = context.getInitParameter(key);
                context.setAttribute(key, value);
                log.info("TestMP settings: " + key.toString() + "=" + value);
            }
        }

        // Customize the host page by locale
        String locale = context.getAttribute("locale").toString();
        log.info("Change host page locale to " + locale);
        try {
            String hostPage = context.getResource("/webconsole/WebConsole.html").getPath();
            log.info("Host page location: " + hostPage);

            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(hostPage), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                sb.append(line).append("\n");
            }
            reader.close();

            String content = sb.toString();
            String fmt1 = "<meta name=\"gwt:property\" content=\"locale=%s\">";
            content = content.replaceAll(String.format(fmt1, ".+?"), String.format(fmt1, locale));
            String fmt2 = "<title>%s</title>";
            content = content.replaceAll(String.format(fmt2, ".+?"), String.format(fmt2, getTitle(locale)));

            PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(hostPage), "UTF-8"));
            writer.print(content);
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Start the task runner
        int size = Integer.parseInt(context.getAttribute("executionThreadNum").toString());
        long timeout = Long.parseLong(context.getAttribute("executionTimeout").toString());
        String traceFileDir = null;
        try {
            traceFileDir = context.getResource("/webconsole/executions").getPath();
            context.setAttribute("traceFileDir", traceFileDir);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        TaskRunner taskRunner = new TaskRunner(size, timeout * 1000, traceFileDir);
        context.setAttribute("taskRunner", taskRunner);
        taskRunner.start();

        // Start the task scheduler
        Timer scheduleTimer = new Timer(true);
        String testEnvStoreUrl = (String) context.getAttribute("testEnvStoreUrl");
        long refreshingGap = Long.parseLong((String) context.getAttribute("scheduleRefreshingTimeGap")) * 1000;
        long triggerLatency = Long.parseLong((String) context.getAttribute("taskTriggerMaxLatency")) * 1000;
        TaskScheduler taskScheduler = new TaskScheduler(testEnvStoreUrl, refreshingGap, taskRunner, triggerLatency);
        scheduleTimer.scheduleAtFixedRate(taskScheduler, 0, 1000);
    }

    private String getTitle(String locale) {
        if (locale.startsWith("zh")) {
            return "TestMP 控制台";
        }
        return "TestMP Web Console";
    }
}
