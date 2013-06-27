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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.Properties;

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

        int size = Integer.parseInt(context.getAttribute("executionThreadNum").toString());
        long timeout = Long.parseLong(context.getAttribute("executionTimeout").toString());
        String traceFileDir = null;
        try {
            traceFileDir = context.getResource("/webconsole/executions").getPath();
            context.setAttribute("traceFileDir", traceFileDir);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        ExecutionRunner executor = new ExecutionRunner(size, timeout * 1000, traceFileDir);
        context.setAttribute("executionRunner", executor);
        executor.start();
    }
}
