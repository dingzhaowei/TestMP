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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.testmp.datastore.client.DataInfo;
import org.testmp.datastore.client.DataStoreClient;
import org.testmp.webconsole.server.Filter.Criteria;

@SuppressWarnings("serial")
public class ReportService extends HttpServlet {

    private static final String TEST_METRICS_REPORT = "Test Metrics";

    private static final String DATA_ANALYTICS_REPORT = "Data Analytics";

    private static final String ENVIRONMENT_STATUS_REPORT = "Environment Status";

    private static Logger log = Logger.getLogger(ReportService.class);

    private String baseDir;

    private String baseUrl;

    private String serviceName;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        initServiceInfo(req);

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
        String reportType = params.get("reportType");
        String action = params.get("action");

        try {
            if (action.equals("create")) {
                String reportFileName = null;
                if (reportType.equals(TEST_METRICS_REPORT)) {
                    reportFileName = generateTestMetricsReport(params);
                } else if (reportType.equals(DATA_ANALYTICS_REPORT)) {
                    reportFileName = generateDataAnalyticsReport(params);
                } else if (reportType.equals(ENVIRONMENT_STATUS_REPORT)) {
                    reportFileName = generateEnvStatusReport(params);
                }
                output.print(reportFileName);
                output.flush();
            } else if (action.equals("update")) {
                String filename = params.get("filename");
                String replacement = params.get("replacement");
                updateReportContent(filename, replacement);
            } else if (action.equals("send")) {
                sendReport(params);
            } else if (action.equals("getCustomSetting")) {
                Map<String, String> settings = getCustomSetting(reportType);
                ObjectMapper mapper = new ObjectMapper();
                output.print(mapper.writeValueAsString(settings));
                output.flush();
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Action is not supported");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServletException(e);
        }
    }

    private void initServiceInfo(HttpServletRequest req) {
        String path = req.getServletPath();
        String scheme = req.getScheme();
        String serverName = req.getServerName();
        int serverPort = req.getServerPort();
        String contextPath = req.getContextPath();

        serviceName = path.isEmpty() ? "" : path.substring(path.lastIndexOf('/') + 1);
        baseDir = path.isEmpty() ? "/" : path.substring(0, path.indexOf('/', 1));
        baseUrl = String.format("%s://%s:%d%s%s", scheme, serverName, serverPort, contextPath, baseDir);
    }

    private File getReportFile(String filename) {
        File reportFile = new File(getRealPath(baseDir + "/reports/" + filename));
        return reportFile;
    }

    private File getSignedReportFile(String filename) {
        filename = "signed" + filename;
        File signedoffReport = new File(getRealPath(baseDir + "/reports") + File.separator + filename);
        return signedoffReport;
    }

    private String getReportContent(File reportFile) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(reportFile), "UTF-8"));
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
        return content;
    }

    private Map<String, String> getCustomSetting(String reportType) {
        Map<String, String> settings = new HashMap<String, String>();
        Object recipients = null, subject = null;
        if (reportType.equals(TEST_METRICS_REPORT)) {
            recipients = getServletContext().getAttribute("testMetricsReportRecipients");
            subject = getServletContext().getAttribute("testMetricsReportSubject");
        } else if (reportType.equals(DATA_ANALYTICS_REPORT)) {
            recipients = getServletContext().getAttribute("dataAnalyticsReportRecipients");
            subject = getServletContext().getAttribute("dataAnalyticsReportSubject");
        } else if (reportType.equals(ENVIRONMENT_STATUS_REPORT)) {
            recipients = getServletContext().getAttribute("envStatusReportRecipients");
            subject = getServletContext().getAttribute("envStatusReportSubject");
        }
        Object smtphost = getServletContext().getAttribute("smtpSettingHost");
        Object smtpport = getServletContext().getAttribute("smtpSettingPort");
        Object username = getServletContext().getAttribute("smtpSettingUser");
        Object password = getServletContext().getAttribute("smtpSettingPass");
        Object starttls = getServletContext().getAttribute("smtpSettingSTARTTLS");
        settings.put("recipients", recipients == null ? "" : recipients.toString().trim());
        settings.put("subject", subject == null ? "" : subject.toString().trim());
        settings.put("smtphost", smtphost == null ? "" : smtphost.toString().trim());
        settings.put("smtpport", smtpport == null ? "25" : smtpport.toString().trim());
        settings.put("starttls", starttls == null ? "false" : starttls.toString().trim());
        settings.put("username", username == null ? "" : username.toString().trim());
        settings.put("password", password == null ? "" : password.toString().trim());
        return settings;
    }

    private String generateTestMetricsReport(HashMap<String, String> params) throws Exception {
        VelocityEngine ve = new VelocityEngine();
        initVelocityEngine(ve);

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Map<String, Object>> testMetricsTable = mapper.readValue(params.get("testMetricsTable"),
                new TypeReference<Map<String, Map<String, Object>>>() {
                });

        for (Map.Entry<String, Map<String, Object>> row : testMetricsTable.entrySet()) {
            Map<String, Object> testMetrics = row.getValue();
            double avgTime = ((Number) testMetrics.get("avgTime")).doubleValue();
            testMetrics.put("avgTime", Math.round(avgTime));
            double alwaysBadPercentage = ((Number) testMetrics.get("alwaysBadPercentage")).doubleValue();
            testMetrics.put("alwaysBadPercentage", String.format("%.1f%%", alwaysBadPercentage));
            double degradingPercentage = ((Number) testMetrics.get("degradingPercentage")).doubleValue();
            testMetrics.put("degradingPercentage", String.format("%.1f%%", degradingPercentage));
            double upgradingPercentage = ((Number) testMetrics.get("upgradingPercentage")).doubleValue();
            testMetrics.put("upgradingPercentage", String.format("%.1f%%", upgradingPercentage));
            double alwaysGoodPercentage = ((Number) testMetrics.get("alwaysGoodPercentage")).doubleValue();
            testMetrics.put("alwaysGoodPercentage", String.format("%.1f%%", alwaysGoodPercentage));
            double minVolatility = ((Number) testMetrics.get("minVolatility")).doubleValue();
            testMetrics.put("minVolatility", String.format("%.3f", minVolatility));
            double maxVolatility = ((Number) testMetrics.get("maxVolatility")).doubleValue();
            testMetrics.put("maxVolatility", String.format("%.3f", maxVolatility));
        }

        File reportDir = new File(getRealPath(baseDir + "/reports"));
        File reportFile = File.createTempFile("testMetricsReport", ".html", reportDir);
        Writer writer = new OutputStreamWriter(new FileOutputStream(reportFile), "UTF-8");

        try {
            VelocityContext context = new VelocityContext();
            context.put("testMetricsTable", testMetricsTable);
            context.put("baseUrl", baseUrl);
            context.put("serviceName", serviceName);
            context.put("filename", reportFile.getName());
            context.put("messages", getLocalizedMessages());
            Template template = ve.getTemplate("testMetricsReport.vm", "UTF-8");
            template.merge(context, writer);
            return reportFile.getName();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private String generateDataAnalyticsReport(HashMap<String, String> params) throws Exception {
        VelocityEngine ve = new VelocityEngine();
        initVelocityEngine(ve);

        ObjectMapper mapper = new ObjectMapper();
        Criteria criteria = Criteria.valueOf(mapper.writeValueAsString(params.get("testDataCriteria")));

        String testDataStoreUrl = (String) getServletContext().getAttribute("testDataStoreUrl");
        DataAssemblyStrategy strategy = new TestDataAssemblyStrategy();
        DataLoader<Map> loader = new DataLoader<Map>(testDataStoreUrl, Map.class, strategy);
        List<Map<String, Object>> dataList = loader.load();

        if (criteria != null) {
            Filter filter = new Filter(criteria);
            dataList = filter.doFilter(dataList);
        }

        File reportDir = new File(getRealPath(baseDir + "/reports"));
        File reportFile = File.createTempFile("dataAnalyticsReport", ".html", reportDir);
        Writer writer = new OutputStreamWriter(new FileOutputStream(reportFile), "UTF-8");

        Map<String, Object> dataAnalyticsResult = new HashMap<String, Object>();
        dataAnalyticsResult.put("totalData", dataList.size());

        try {
            VelocityContext context = new VelocityContext();
            context.put("dataAnalyticsResult", dataAnalyticsResult);
            context.put("baseUrl", baseUrl);
            context.put("messages", getLocalizedMessages());
            Template template = ve.getTemplate("dataAnalyticsReport.vm", "UTF-8");
            template.merge(context, writer);
            return reportFile.getName();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private String generateEnvStatusReport(HashMap<String, String> params) throws Exception {
        VelocityEngine ve = new VelocityEngine();
        initVelocityEngine(ve);

        ObjectMapper mapper = new ObjectMapper();
        List<String> envNames = mapper.readValue(params.get("environments"), new TypeReference<List<String>>() {
        });
        DataStoreClient client = new DataStoreClient((String) getServletContext().getAttribute("testEnvStoreUrl"));
        List<DataInfo<Map>> testEnvInfoList = client.getDataByTag(Map.class, "TestEnv");
        List<DataInfo<Map>> taskInfoList = client.getDataByTag(Map.class, "Task");

        Map envStatusTable = new LinkedHashMap();
        for (DataInfo<Map> info : testEnvInfoList) {
            Map m = info.getData();
            String envName = (String) m.get("envName");
            String refUrl = (String) m.get("refUrl");
            if (!envNames.contains(envName)) {
                continue;
            }
            envStatusTable.put(envName, new LinkedHashMap());
            ((Map) envStatusTable.get(envName)).put("refUrl", refUrl == null ? "" : refUrl);
            ((Map) envStatusTable.get(envName)).put("tasks", new LinkedHashMap());
        }

        for (DataInfo<Map> info : taskInfoList) {
            Map m = info.getData();
            String envName = (String) m.get("envName");
            if (!envNames.contains(envName)) {
                continue;
            }
            String taskName = (String) m.get("taskName");
            String taskStatus = (String) m.get("status");
            Long lastRunTime = (Long) m.get("lastRunTime");
            Map tasks = (Map) ((Map) envStatusTable.get(envName)).get("tasks");
            tasks.put(taskName, new LinkedHashMap());
            ((Map) tasks.get(taskName)).put("taskStatus", taskStatus == null ? "" : taskStatus);
            ((Map) tasks.get(taskName)).put("lastRunTime", lastRunTime == null ? "" : formatTime(lastRunTime));
        }

        File reportDir = new File(getRealPath(baseDir + "/reports"));
        File reportFile = File.createTempFile("envStatusReport", ".html", reportDir);
        Writer writer = new OutputStreamWriter(new FileOutputStream(reportFile), "UTF-8");

        try {
            VelocityContext context = new VelocityContext();
            context.put("envStatusTable", envStatusTable);
            context.put("baseUrl", baseUrl);
            context.put("messages", getLocalizedMessages());
            Template template = ve.getTemplate("envStatusReport.vm", "UTF-8");
            template.merge(context, writer);
            return reportFile.getName();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private void initVelocityEngine(VelocityEngine ve) {
        String reportTemplatesDir = getRealPath(baseDir + "/templates");
        ve.setProperty(VelocityEngine.FILE_RESOURCE_LOADER_PATH, reportTemplatesDir);

        String home = System.getenv("TESTMP_HOME");
        if (home != null) {
            String sep = File.separator;
            String velocityLog = home + sep + "log" + sep + "velocity.log";
            ve.setProperty(VelocityEngine.RUNTIME_LOG, velocityLog);
        }

        ve.setProperty(VelocityEngine.INPUT_ENCODING, "UTF-8");
        ve.setProperty(VelocityEngine.OUTPUT_ENCODING, "UTF-8");
        ve.init();
    }

    private String formatTime(Long time) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(new Date(time));
    }

    private void updateReportContent(String filename, String replacement) throws Exception {
        File reportFile = getReportFile(filename);

        String content = getReportContent(reportFile);

        Pattern p = Pattern.compile("<script>.*?</script>", Pattern.DOTALL);
        Matcher m = p.matcher(content);
        if (m != null) {
            content = m.replaceAll("");
        }

        p = Pattern.compile("<body>.*?</body>", Pattern.DOTALL);
        m = p.matcher(content);
        if (m != null) {
            content = m.replaceAll(Matcher.quoteReplacement(("<body>\n" + replacement + "\n</body>")));
        }

        File signedReportFile = getSignedReportFile(filename);
        Writer writer = new OutputStreamWriter(new FileOutputStream(signedReportFile), "UTF-8");
        writer.write(content);
        writer.close();
    }

    private void sendReport(HashMap<String, String> params) throws Exception {
        Properties props = new Properties(System.getProperties());
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", true);
        props.put("mail.smtp.ssl.trust", "*");
        props.put("mail.smtp.host", params.get("smtphost"));
        props.put("mail.smtp.port", Integer.parseInt(params.get("smtpport")));
        props.put("mail.smtp.starttls.enable", Boolean.parseBoolean(params.get("starttls")));

        Session mailSession = Session.getInstance(props);
        Transport transport = mailSession.getTransport();
        MimeMessage message = new MimeMessage(mailSession);
        message.setSubject(params.get("subject"), "utf-8");
        message.setFrom(new InternetAddress(params.get("username")));

        String filename = params.get("filename");
        String content = null;
        File signedReportFile = getSignedReportFile(filename);
        if (signedReportFile.exists()) {
            content = getReportContent(signedReportFile);
        } else {
            File reportFile = getReportFile(filename);
            content = getReportContent(reportFile);
        }

        String comment = String.format("<br><pre>%s</pre>", params.get("comment"));
        int p = content.indexOf("<body>") + 6;
        content = content.substring(0, p) + comment + content.substring(p);

        String testmpUrl = baseUrl.substring(0, baseUrl.lastIndexOf('/'));
        String footer = String.format("Generated by <a href=\"%s\">TestMP</a>, click to see the detail", testmpUrl);
        p = content.lastIndexOf("</body>");
        content = content.substring(0, p) + footer + content.substring(p);

        message.setContent(content, "text/html; charset=utf-8");
        List<InternetAddress> recipients = new ArrayList<InternetAddress>();
        for (String recipient : params.get("recipients").split(";")) {
            recipients.add(new InternetAddress(recipient.trim()));
        }
        message.addRecipients(Message.RecipientType.TO, recipients.toArray(new InternetAddress[0]));

        transport.connect(params.get("username"), params.get("password"));
        transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO));
        transport.close();
    }

    private String getRealPath(String path) {
        try {
            return getServletContext().getResource(path).getPath();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> getLocalizedMessages() {
        Map<String, String> messages = new HashMap<String, String>();
        ResourceBundle bundle = (ResourceBundle) getServletContext().getAttribute("messages");
        for (String key : bundle.keySet()) {
            messages.put(key, bundle.getString(key));
        }
        return messages;
    }
}
