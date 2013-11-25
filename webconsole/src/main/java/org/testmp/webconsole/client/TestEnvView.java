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

package org.testmp.webconsole.client;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.testmp.webconsole.client.ReportWindow.ReportType;
import org.testmp.webconsole.shared.ClientConfig;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Timer;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.OperationBinding;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.RestDataSource;
import com.smartgwt.client.data.fields.DataSourceBooleanField;
import com.smartgwt.client.data.fields.DataSourceImageField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSOperationType;
import com.smartgwt.client.types.DSProtocol;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.ImgButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.Layout;
import com.smartgwt.client.widgets.layout.VLayout;

public class TestEnvView extends VLayout {

    private ListGrid testEnvGrid;

    private DataSource testEnvSource = new TestEnvSource();

    private DataSource taskSource = new TaskSource();

    private DataSource executionSource = new ExecutionSource();

    private DataSource hostSource = new HostSource();

    @Override
    protected void onInit() {
        testEnvGrid = new ListGrid() {
            @Override
            protected Canvas createRecordComponent(final ListGridRecord record, Integer colNum) {

                String fieldName = this.getFieldName(colNum);

                if (fieldName.equals("task")) {
                    HLayout recordCanvas = new HLayout();
                    recordCanvas.setHeight(22);
                    recordCanvas.setAlign(Alignment.CENTER);
                    ImgButton taskImg = new ImgButton();
                    taskImg.setShowDown(false);
                    taskImg.setShowRollOver(false);
                    taskImg.setSrc("task.png");
                    taskImg.setPrompt(ClientConfig.messages.viewTasks());
                    taskImg.setHeight(16);
                    taskImg.setWidth(16);
                    taskImg.setLayoutAlign(Alignment.CENTER);
                    taskImg.addClickHandler(new ClickHandler() {
                        public void onClick(ClickEvent event) {
                            TasksWindow window = new TasksWindow(record);
                            window.show();
                            window.scheduleTaskRefreshing();
                        }
                    });
                    recordCanvas.addMember(taskImg);
                    return recordCanvas;
                } else {
                    return super.createRecordComponent(record, colNum);
                }

            }
        };
        testEnvGrid.setWidth("99%");
        testEnvGrid.setLayoutAlign(Alignment.CENTER);
        testEnvGrid.setShowRollOver(false);
        testEnvGrid.setShowRecordComponents(true);
        testEnvGrid.setShowRecordComponentsByCell(true);
        testEnvGrid.setShowFilterEditor(true);
        testEnvGrid.setCanRemoveRecords(true);
        testEnvGrid.setWarnOnRemoval(true);
        testEnvGrid.setCanEdit(true);
        testEnvGrid.setAutoFetchData(true);
        testEnvGrid.setDataSource(testEnvSource);

        ListGridField envIdField = new ListGridField("envId");
        envIdField.setHidden(true);

        ListGridField envNameField = new ListGridField("envName", ClientConfig.messages.environment());
        envNameField.setRequired(true);

        ListGridField refUrlField = new ListGridField("refUrl", ClientConfig.messages.referencePage());
        refUrlField.setType(ListGridFieldType.LINK);
        refUrlField.setCanFilter(false);

        ListGridField taskField = new ListGridField("task", ClientConfig.messages.tasks());
        taskField.setCanFilter(false);
        taskField.setCanEdit(false);

        testEnvGrid.setFields(envIdField, envNameField, refUrlField, taskField);
        addMember(testEnvGrid);

        HLayout controls = new HLayout();
        controls.setSize("99%", "20");
        controls.setMargin(10);
        controls.setMembersMargin(5);
        controls.setLayoutAlign(Alignment.CENTER);
        controls.setAlign(Alignment.RIGHT);

        IButton newEnvButton = new IButton(ClientConfig.messages.new_());
        newEnvButton.setIcon("newenv.png");
        newEnvButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                testEnvGrid.startEditingNew();
            }

        });
        controls.addMember(newEnvButton);

        IButton hostsButton = new IButton(ClientConfig.messages.hosts());
        hostsButton.setIcon("hosts.png");
        hostsButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                HostsWindow window = new HostsWindow();
                window.show();
            }

        });
        controls.addMember(hostsButton);

        IButton reloadButton = new IButton(ClientConfig.messages.reload());
        reloadButton.setIcon("reload.png");
        reloadButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                testEnvGrid.invalidateCache();
            }

        });
        controls.addMember(reloadButton);

        IButton reportButton = new IButton(ClientConfig.messages.report());
        reportButton.setIcon("report.png");
        reportButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                Map<String, Object> params = new HashMap<String, Object>();
                JSONArray a = new JSONArray();
                int i = 0;
                for (ListGridRecord record : testEnvGrid.getRecords()) {
                    a.set(i, new JSONString(record.getAttribute("envName")));
                    i++;
                }
                params.put("action", "create");
                params.put("environments", a.toString());
                ReportWindow reportWindow = new ReportWindow();
                reportWindow.showReport(ReportType.ENVIRONMENT_STATUS, params);
            }

        });
        controls.addMember(reportButton);

        addMember(controls);
    }

    private class TestEnvSource extends RestDataSource {

        TestEnvSource() {
            setID("testEnvDS");

            DataSourceIntegerField envIdField = new DataSourceIntegerField("envId");
            envIdField.setPrimaryKey(true);

            DataSourceTextField envNameField = new DataSourceTextField("envName");
            DataSourceTextField refUrlField = new DataSourceTextField("refUrl");
            setFields(envIdField, envNameField, refUrlField);

            setDataFormat(DSDataFormat.JSON);
            setClientOnly(false);

            String baseUrl = GWT.getModuleBaseURL();
            String servicePath = ClientConfig.constants.testEnvService();
            String requestUrl = baseUrl + servicePath.substring(servicePath.lastIndexOf('/') + 1);
            setDataURL(requestUrl);

            OperationBinding fetch = new OperationBinding();
            fetch.setOperationType(DSOperationType.FETCH);
            fetch.setDataProtocol(DSProtocol.POSTMESSAGE);
            fetch.setDataFormat(DSDataFormat.JSON);
            OperationBinding add = new OperationBinding();
            add.setOperationType(DSOperationType.ADD);
            add.setDataProtocol(DSProtocol.POSTMESSAGE);
            OperationBinding update = new OperationBinding();
            update.setOperationType(DSOperationType.UPDATE);
            update.setDataProtocol(DSProtocol.POSTMESSAGE);
            OperationBinding remove = new OperationBinding();
            remove.setOperationType(DSOperationType.REMOVE);
            remove.setDataProtocol(DSProtocol.POSTMESSAGE);
            setOperationBindings(fetch, add, update, remove);
        }

    }

    private class TasksWindow extends Window {

        ListGrid taskGrid;

        Timer taskRefreshing;

        TasksWindow(final ListGridRecord envRecord) {
            setWidth(750);
            setHeight(260);
            setTitle(envRecord.getAttribute("envName"));
            setShowMinimizeButton(false);
            setIsModal(true);
            setShowModalMask(true);
            centerInPage();
            addCloseClickHandler(new CloseClickHandler() {
                public void onCloseClick(CloseClickEvent event) {
                    if (taskRefreshing != null) {
                        taskRefreshing.cancel();
                    }
                    TasksWindow.this.destroy();
                }
            });

            VLayout layout = new VLayout();
            layout.setMargin(5);
            layout.setMembersMargin(5);

            taskGrid = new ListGrid() {

                @Override
                protected Canvas createRecordComponent(final ListGridRecord record, Integer colNum) {

                    String fieldName = this.getFieldName(colNum);

                    if (fieldName.equals("execution")) {
                        HLayout recordCanvas = new HLayout();
                        recordCanvas.setWidth100();
                        recordCanvas.setHeight(22);
                        recordCanvas.setAlign(Alignment.CENTER);
                        ImgButton executionImg = new ImgButton();
                        executionImg.setShowDown(false);
                        executionImg.setShowRollOver(false);
                        executionImg.setSrc("execution.png");
                        executionImg.setPrompt(ClientConfig.messages.viewExecutions());
                        executionImg.setHeight(16);
                        executionImg.setWidth(16);
                        executionImg.setLayoutAlign(Alignment.CENTER);
                        executionImg.addClickHandler(new ClickHandler() {
                            public void onClick(ClickEvent event) {
                                ExecutionsWindow window = new ExecutionsWindow(record);
                                window.show();
                            }
                        });
                        recordCanvas.addMember(executionImg);
                        return recordCanvas;
                    } else if (fieldName.equals("run")) {
                        HLayout recordCanvas = new HLayout();
                        recordCanvas.setWidth100();
                        recordCanvas.setHeight(22);
                        recordCanvas.setAlign(Alignment.CENTER);

                        String cancel = ClientConfig.messages.cancel();
                        String run = ClientConfig.messages.run();
                        IButton taskControl = new IButton(isTaskRunning(record) ? cancel : run);
                        taskControl.setMargin(2);
                        taskControl.addClickHandler(new ClickHandler() {
                            public void onClick(ClickEvent event) {
                                runTask(record);
                            }
                        });
                        recordCanvas.addMember(taskControl);
                        return recordCanvas;
                    } else {
                        return super.createRecordComponent(record, colNum);
                    }

                }
            };

            taskGrid.setWidth("99%");
            taskGrid.setCellHeight(30);
            taskGrid.setLayoutAlign(Alignment.CENTER);
            taskGrid.setShowRollOver(false);
            taskGrid.setShowRecordComponents(true);
            taskGrid.setShowRecordComponentsByCell(true);
            taskGrid.setCanRemoveRecords(true);
            taskGrid.setWarnOnRemoval(true);
            taskGrid.setCanEdit(true);
            taskGrid.setDataSource(taskSource);

            ListGridField taskIdField = new ListGridField("taskId");
            taskIdField.setHidden(true);

            ListGridField taskNameField = new ListGridField("taskName", ClientConfig.messages.task());
            taskNameField.setRequired(true);
            taskNameField.setShowHover(true);

            ListGridField executionField = new ListGridField("execution", ClientConfig.messages.execution(), 80);
            executionField.setCanEdit(false);
            executionField.setCellFormatter(new CellFormatter() {

                @Override
                public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                    return "";
                }

            });

            ListGridField statusField = new ListGridField("status", ClientConfig.messages.status(), 40);
            statusField.setType(ListGridFieldType.IMAGE);
            statusField.setCellAlign(Alignment.CENTER);
            statusField.setCanEdit(false);

            ListGridField runField = new ListGridField("run", ClientConfig.messages.run(), 150);
            runField.setCanEdit(false);
            runField.setShowTitle(false);

            ListGridField scheduleField = new ListGridField("schedule", ClientConfig.messages.schedule(), 150);
            scheduleField.setShowHover(true);

            ListGridField lastRunTimeField = new ListGridField("lastRunTime", ClientConfig.messages.lastRunTime(), 150);
            lastRunTimeField.setType(ListGridFieldType.DATETIME);
            lastRunTimeField.setCanEdit(false);
            lastRunTimeField.setCellFormatter(new CellFormatter() {

                @SuppressWarnings("deprecation")
                @Override
                public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                    if (value == null) {
                        return null;
                    }
                    Date date = new Date(Long.parseLong(value.toString()));
                    String[] elem = new String[6];
                    elem[0] = String.valueOf(1900 + date.getYear());
                    elem[1] = String.valueOf(date.getMonth() + 1);
                    elem[2] = String.valueOf(date.getDate());
                    elem[3] = String.valueOf(date.getHours());
                    elem[4] = String.valueOf(date.getMinutes());
                    elem[5] = String.valueOf(date.getSeconds());
                    for (int i = 1; i < elem.length; i++) {
                        if (elem[i].length() == 1) {
                            elem[i] = "0" + elem[i];
                        }
                    }
                    return elem[0] + "-" + elem[1] + "-" + elem[2] + " " + elem[3] + ":" + elem[4] + ":" + elem[5];
                }

            });

            taskGrid.setFields(taskIdField, taskNameField, executionField, statusField, runField, scheduleField,
                    lastRunTimeField);

            taskGrid.fetchRelatedData(envRecord, testEnvSource);

            layout.addMember(taskGrid);

            HLayout controls = new HLayout(10);
            controls.setSize("99%", "20");
            controls.setMembersMargin(5);
            controls.setLayoutAlign(Alignment.CENTER);
            controls.setAlign(Alignment.CENTER);

            IButton newTaskButton = new IButton(ClientConfig.messages.new_());
            newTaskButton.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    String currentEditingEnv = envRecord.getAttribute("envName");
                    TaskSource taskSource = (TaskSource) taskGrid.getDataSource();
                    taskSource.setCurrentEditingEnv(currentEditingEnv);
                    taskGrid.startEditingNew();
                }
            });
            controls.addMember(newTaskButton);

            layout.addMember(controls);
            addItem(layout);
        }

        private void runTask(ListGridRecord record) {
            StringBuilder dataBuilder = new StringBuilder();
            dataBuilder.append("action=" + (isTaskRunning(record) ? "cancel" : "run"));
            dataBuilder.append("&taskId=").append(URL.encode(record.getAttribute("taskId")));

            final String baseUrl = GWT.getModuleBaseURL();
            String servicePath = ClientConfig.constants.envTaskService();
            String requestUrl = baseUrl + servicePath.substring(servicePath.lastIndexOf('/') + 1);

            RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, requestUrl);
            try {
                builder.sendRequest(dataBuilder.toString(), new RequestCallback() {

                    @Override
                    public void onResponseReceived(Request request, Response response) {
                        if (response.getStatusCode() != Response.SC_OK) {
                            SC.warn(response.getStatusCode() + " - " + response.getStatusText());
                        }
                    }

                    @Override
                    public void onError(Request request, Throwable exception) {
                        SC.warn(exception.getMessage());
                    }

                });
            } catch (Exception e) {
                SC.warn(e.getMessage());
            }
        }

        private void refreshTasks() {
            StringBuilder dataBuilder = new StringBuilder();
            dataBuilder.append("action=queryTaskStatus");
            dataBuilder.append("&taskIds=");

            ListGridRecord[] records = taskGrid.getRecords();
            if (records.length == 0) {
                scheduleTaskRefreshing();
                return;
            }

            for (int i = 0; i < records.length; i++) {
                if (i > 0) {
                    dataBuilder.append(',');
                }
                dataBuilder.append(records[i].getAttribute("taskId"));
            }

            final String baseUrl = GWT.getModuleBaseURL();
            String servicePath = ClientConfig.constants.envTaskService();
            String requestUrl = baseUrl + servicePath.substring(servicePath.lastIndexOf('/') + 1);

            RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, requestUrl);
            try {
                builder.sendRequest(dataBuilder.toString(), new RequestCallback() {

                    @Override
                    public void onResponseReceived(Request request, Response response) {
                        Map<Integer, String> statusMap = new HashMap<Integer, String>();

                        if (response.getStatusCode() == Response.SC_OK) {
                            String resp = response.getText();

                            if (resp.isEmpty()) {
                                scheduleTaskRefreshing();
                                return;
                            }

                            for (String status : resp.split(",")) {
                                String[] keyValue = status.split("=");
                                statusMap.put(Integer.parseInt(keyValue[0]), keyValue[1]);
                            }

                            for (ListGridRecord record : taskGrid.getRecords()) {
                                Integer taskId = record.getAttributeAsInt("taskId");
                                String lastStatus = record.getAttribute("status");
                                String lastLastRunTime = record.getAttribute("lastRunTime");

                                if (!statusMap.containsKey(taskId)) {
                                    continue;
                                }

                                String[] valuePair = statusMap.get(taskId).split(";");
                                String currStatus = valuePair[0].equals("null") ? null : valuePair[0];
                                String currLastRunTime = valuePair[1].equals("null") ? null : valuePair[1];

                                if ((currStatus != null && !currStatus.equals(lastStatus))
                                        || (currLastRunTime != null && !currLastRunTime.equals(lastLastRunTime))) {
                                    record.setAttribute("status", currStatus);
                                    record.setAttribute("lastRunTime", currLastRunTime);
                                    int rowNum = taskGrid.getRecordIndex(record);
                                    int colNum = taskGrid.getFieldNum("run");
                                    Layout recordComp = (Layout) taskGrid.getRecordComponent(rowNum, colNum);
                                    IButton taskControl = (IButton) recordComp.getMember(0);
                                    String cancel = ClientConfig.messages.cancel();
                                    String run = ClientConfig.messages.run();
                                    taskControl.setTitle(isTaskRunning(record) ? cancel : run);
                                    taskGrid.refreshRow(rowNum);
                                }
                            }

                            scheduleTaskRefreshing();
                        }
                    }

                    @Override
                    public void onError(Request request, Throwable exception) {
                        SC.warn(exception.getMessage());
                    }

                });

            } catch (Exception e) {
                SC.warn(e.getMessage());
            }
        }

        private boolean isTaskRunning(Record record) {
            String status = record.getAttribute("status");
            return status != null && status.startsWith("running");
        }

        private void scheduleTaskRefreshing() {
            taskRefreshing = new Timer() {
                @Override
                public void run() {
                    refreshTasks();
                }
            };
            taskRefreshing.schedule(500);
        }

    }

    private class TaskSource extends RestDataSource {

        String currentEditingEnv;

        TaskSource() {
            setID("taskDS");

            DataSourceIntegerField taskIdField = new DataSourceIntegerField("taskId");
            taskIdField.setPrimaryKey(true);

            DataSourceTextField envNameField = new DataSourceTextField("envName");
            envNameField.setForeignKey("testEnvDS.envName");

            DataSourceTextField taskNameField = new DataSourceTextField("taskName");
            DataSourceImageField statusField = new DataSourceImageField("status");
            DataSourceTextField scheduleField = new DataSourceTextField("schedule");
            DataSourceTextField lastRunTimeField = new DataSourceTextField("lastRunTime");

            setFields(taskIdField, taskNameField, envNameField, statusField, scheduleField, lastRunTimeField);

            setDataFormat(DSDataFormat.JSON);
            setClientOnly(false);

            String baseUrl = GWT.getModuleBaseURL();
            String servicePath = ClientConfig.constants.testEnvService();
            String requestUrl = baseUrl + servicePath.substring(servicePath.lastIndexOf('/') + 1);
            setDataURL(requestUrl);

            OperationBinding fetch = new OperationBinding();
            fetch.setOperationType(DSOperationType.FETCH);
            fetch.setDataProtocol(DSProtocol.POSTMESSAGE);
            fetch.setDataFormat(DSDataFormat.JSON);
            OperationBinding add = new OperationBinding();
            add.setOperationType(DSOperationType.ADD);
            add.setDataProtocol(DSProtocol.POSTMESSAGE);
            OperationBinding update = new OperationBinding();
            update.setOperationType(DSOperationType.UPDATE);
            update.setDataProtocol(DSProtocol.POSTMESSAGE);
            OperationBinding remove = new OperationBinding();
            remove.setOperationType(DSOperationType.REMOVE);
            remove.setDataProtocol(DSProtocol.POSTMESSAGE);
            setOperationBindings(fetch, add, update, remove);
        }

        void setCurrentEditingEnv(String currentEditingEnv) {
            this.currentEditingEnv = currentEditingEnv;
        }

        @Override
        protected Object transformRequest(DSRequest dsRequest) {
            if (dsRequest.getOperationType() == DSOperationType.ADD) {
                Record record = new Record(dsRequest.getData());
                record.setAttribute("envName", currentEditingEnv);
                dsRequest.setData(record);
            }
            return super.transformRequest(dsRequest);
        }
    }

    private class ExecutionsWindow extends Window {

        ExecutionsWindow(final ListGridRecord taskRecord) {
            setWidth(780);
            setHeight(260);
            setTitle(taskRecord.getAttribute("taskName"));
            setShowMinimizeButton(false);
            setIsModal(true);
            centerInPage();
            addCloseClickHandler(new CloseClickHandler() {
                public void onCloseClick(CloseClickEvent event) {
                    ExecutionsWindow.this.destroy();
                }
            });

            final ListGrid executionGrid = new ListGrid() {

                @Override
                protected Canvas createRecordComponent(final ListGridRecord record, Integer colNum) {

                    String fieldName = this.getFieldName(colNum);

                    if (fieldName.equals("trace")) {
                        HLayout recordCanvas = new HLayout();
                        recordCanvas.setWidth100();
                        recordCanvas.setHeight(22);
                        recordCanvas.setAlign(Alignment.CENTER);
                        ImgButton traceImg = new ImgButton();
                        traceImg.setShowDown(false);
                        traceImg.setShowRollOver(false);
                        traceImg.setSrc("trace.png");
                        traceImg.setPrompt(ClientConfig.messages.viewTrace());
                        traceImg.setHeight(16);
                        traceImg.setWidth(16);
                        traceImg.setLayoutAlign(Alignment.CENTER);
                        traceImg.addClickHandler(new ClickHandler() {
                            public void onClick(ClickEvent event) {
                                TraceWindow window = new TraceWindow();
                                window.showTrace(record.getAttributeAsInt("executionId"));
                            }
                        });
                        recordCanvas.addMember(traceImg);
                        return recordCanvas;
                    } else {
                        return super.createRecordComponent(record, colNum);
                    }
                }

            };

            executionGrid.setWidth("99%");
            executionGrid.setLayoutAlign(Alignment.CENTER);
            executionGrid.setShowRollOver(false);
            executionGrid.setShowRecordComponents(true);
            executionGrid.setShowRecordComponentsByCell(true);
            executionGrid.setCanRemoveRecords(true);
            executionGrid.setWarnOnRemoval(true);
            executionGrid.setCanEdit(true);
            executionGrid.setDataSource(executionSource);

            ListGridField selectedField = new ListGridField("selected", ClientConfig.messages.onOrOff(), 40);
            selectedField.setType(ListGridFieldType.BOOLEAN);
            selectedField.setDefaultValue(true);

            ListGridField hostField = new ListGridField("host", ClientConfig.messages.host(), 150);
            hostField.setRequired(true);
            hostField.setOptionDataSource(hostSource);
            hostField.setDisplayField("hostname");
            hostField.setValueField("hostname");
            hostField.setDefaultValue("localhost");
            hostField.setEditorType(new ComboBoxItem());
            hostField.setShowHover(true);

            ListGridField workingDirField = new ListGridField("workingDir", ClientConfig.messages.workingDir(), 100);
            workingDirField.setShowHover(true);

            ListGridField commandField = new ListGridField("command", ClientConfig.messages.command());
            commandField.setRequired(true);
            commandField.setShowHover(true);

            ListGridField retCodeField = new ListGridField("retCode", ClientConfig.messages.retCode(), 80);
            retCodeField.setType(ListGridFieldType.INTEGER);
            retCodeField.setCanEdit(false);

            ListGridField traceField = new ListGridField("trace", ClientConfig.messages.trace(), 80);
            traceField.setCanEdit(false);

            executionGrid.setFields(selectedField, hostField, workingDirField, commandField, retCodeField, traceField);

            Criteria criteria = new Criteria();
            criteria.addCriteria("envName", taskRecord.getAttribute("envName"));
            criteria.addCriteria("taskName", taskRecord.getAttribute("taskName"));
            executionGrid.fetchData(criteria);

            HLayout controls = new HLayout();
            controls.setSize("99%", "20");
            controls.setMargin(5);
            controls.setMembersMargin(5);
            controls.setLayoutAlign(Alignment.CENTER);
            controls.setAlign(Alignment.CENTER);

            IButton newExecutionButton = new IButton(ClientConfig.messages.new_());
            newExecutionButton.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    String currentEditingEnv = taskRecord.getAttribute("envName");
                    String currentEditingTask = taskRecord.getAttribute("taskName");
                    ExecutionSource executionSource = (ExecutionSource) executionGrid.getDataSource();
                    executionSource.setCurrentEditingEnv(currentEditingEnv);
                    executionSource.setCurrentEditingTask(currentEditingTask);
                    executionGrid.startEditingNew();
                }
            });
            controls.addMember(newExecutionButton);

            VLayout layout = new VLayout();
            layout.setWidth100();
            layout.setHeight100();
            layout.setMargin(5);
            layout.setMembersMargin(5);
            layout.addMember(executionGrid);
            layout.addMember(controls);

            addItem(layout);
        }

    }

    private class ExecutionSource extends RestDataSource {

        String currentEditingEnv;

        String currentEditingTask;

        ExecutionSource() {
            setID("executionDS");

            DataSourceIntegerField executionIdField = new DataSourceIntegerField("executionId");
            executionIdField.setPrimaryKey(true);

            DataSourceTextField envNameField = new DataSourceTextField("envName");
            envNameField.setForeignKey("taskDS.envName");

            DataSourceTextField taskNameField = new DataSourceTextField("taskName");
            taskNameField.setForeignKey("taskDS.taskName");

            DataSourceTextField hostField = new DataSourceTextField("host");
            DataSourceTextField workingDirField = new DataSourceTextField("workingDir");
            DataSourceTextField commandField = new DataSourceTextField("command");
            DataSourceBooleanField selectedField = new DataSourceBooleanField("selected");
            DataSourceIntegerField retCodeField = new DataSourceIntegerField("retCode");
            DataSourceTextField lastRunTimeField = new DataSourceTextField("lastRunTime");
            setFields(executionIdField, selectedField, envNameField, taskNameField, hostField, workingDirField,
                    commandField, retCodeField, lastRunTimeField);

            setDataFormat(DSDataFormat.JSON);
            setClientOnly(false);

            String baseUrl = GWT.getModuleBaseURL();
            String servicePath = ClientConfig.constants.testEnvService();
            String requestUrl = baseUrl + servicePath.substring(servicePath.lastIndexOf('/') + 1);
            setDataURL(requestUrl);

            OperationBinding fetch = new OperationBinding();
            fetch.setOperationType(DSOperationType.FETCH);
            fetch.setDataProtocol(DSProtocol.POSTMESSAGE);
            fetch.setDataFormat(DSDataFormat.JSON);
            OperationBinding add = new OperationBinding();
            add.setOperationType(DSOperationType.ADD);
            add.setDataProtocol(DSProtocol.POSTMESSAGE);
            OperationBinding update = new OperationBinding();
            update.setOperationType(DSOperationType.UPDATE);
            update.setDataProtocol(DSProtocol.POSTMESSAGE);
            OperationBinding remove = new OperationBinding();
            remove.setOperationType(DSOperationType.REMOVE);
            remove.setDataProtocol(DSProtocol.POSTMESSAGE);
            setOperationBindings(fetch, add, update, remove);
        }

        void setCurrentEditingEnv(String currentEditingEnv) {
            this.currentEditingEnv = currentEditingEnv;
        }

        void setCurrentEditingTask(String currentEditingTask) {
            this.currentEditingTask = currentEditingTask;
        }

        @Override
        protected Object transformRequest(DSRequest dsRequest) {
            if (dsRequest.getOperationType() == DSOperationType.ADD) {
                Record record = new Record(dsRequest.getData());
                record.setAttribute("envName", currentEditingEnv);
                record.setAttribute("taskName", currentEditingTask);
                dsRequest.setData(record);
            }
            return super.transformRequest(dsRequest);
        }
    }

    private class HostsWindow extends Window {

        HostsWindow() {
            setWidth(450);
            setHeight(350);
            setTitle(ClientConfig.messages.hosts());
            setShowMinimizeButton(false);
            setIsModal(true);
            setShowModalMask(true);
            centerInPage();
            addCloseClickHandler(new CloseClickHandler() {
                public void onCloseClick(CloseClickEvent event) {
                    HostsWindow.this.destroy();
                }
            });

            VLayout layout = new VLayout();
            layout.setMargin(5);
            layout.setMembersMargin(5);

            final ListGrid hostGrid = new ListGrid();
            hostGrid.setWidth("99%");
            hostGrid.setLayoutAlign(Alignment.CENTER);
            hostGrid.setShowRollOver(false);
            hostGrid.setCanRemoveRecords(true);
            hostGrid.setWarnOnRemoval(true);
            hostGrid.setCanEdit(true);
            hostGrid.setAutoFetchData(true);
            hostGrid.setDataSource(hostSource);

            ListGridField hostNameField = new ListGridField("hostname", ClientConfig.messages.host(), 200);
            hostNameField.setRequired(true);
            ListGridField userNameField = new ListGridField("username", ClientConfig.messages.user(), 100);
            userNameField.setRequired(true);
            ListGridField passwordField = new ListGridField("password", ClientConfig.messages.password(), 100);
            passwordField.setRequired(true);
            passwordField.setCellFormatter(new CellFormatter() {

                @Override
                public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                    if (value == null) {
                        return null;
                    }
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < value.toString().length(); i++) {
                        sb.append('*');
                    }
                    return sb.toString();
                }

            });
            passwordField.setEditorType(new PasswordItem());

            hostGrid.setFields(hostNameField, userNameField, passwordField);
            layout.addMember(hostGrid);

            HLayout controls = new HLayout(10);
            controls.setSize("99%", "20");
            controls.setMargin(5);
            controls.setMembersMargin(5);
            controls.setLayoutAlign(Alignment.CENTER);
            controls.setAlign(Alignment.CENTER);

            IButton newHostButton = new IButton(ClientConfig.messages.new_());
            newHostButton.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    hostGrid.startEditingNew();
                }
            });
            controls.addMember(newHostButton);

            layout.addMember(controls);
            addItem(layout);
        }
    }

    private class HostSource extends RestDataSource {

        HostSource() {
            setID("hostDS");

            DataSourceIntegerField hostIdField = new DataSourceIntegerField("hostId");
            hostIdField.setPrimaryKey(true);

            DataSourceTextField hostNameField = new DataSourceTextField("hostname");
            hostNameField.setRequired(true);

            DataSourceTextField userNameField = new DataSourceTextField("username");
            DataSourceTextField passwordField = new DataSourceTextField("password");
            setFields(hostIdField, hostNameField, userNameField, passwordField);

            setDataFormat(DSDataFormat.JSON);
            setClientOnly(false);

            String baseUrl = GWT.getModuleBaseURL();
            String servicePath = ClientConfig.constants.testEnvService();
            String requestUrl = baseUrl + servicePath.substring(servicePath.lastIndexOf('/') + 1);
            setDataURL(requestUrl);

            OperationBinding fetch = new OperationBinding();
            fetch.setOperationType(DSOperationType.FETCH);
            fetch.setDataProtocol(DSProtocol.POSTMESSAGE);
            fetch.setDataFormat(DSDataFormat.JSON);
            OperationBinding add = new OperationBinding();
            add.setOperationType(DSOperationType.ADD);
            add.setDataProtocol(DSProtocol.POSTMESSAGE);
            OperationBinding update = new OperationBinding();
            update.setOperationType(DSOperationType.UPDATE);
            update.setDataProtocol(DSProtocol.POSTMESSAGE);
            OperationBinding remove = new OperationBinding();
            remove.setOperationType(DSOperationType.REMOVE);
            remove.setDataProtocol(DSProtocol.POSTMESSAGE);
            setOperationBindings(fetch, add, update, remove);
        }
    }

    private class TraceWindow extends Window {

        VLayout windowLayout;

        TraceWindow() {
            setWidth(500);
            setHeight(200);
            setTitle(ClientConfig.messages.trace());
            setShowMaximizeButton(true);
            setCanDragResize(true);
            setIsModal(true);
            setShowModalMask(true);
            centerInPage();
            addCloseClickHandler(new CloseClickHandler() {
                public void onCloseClick(CloseClickEvent event) {
                    TraceWindow.this.destroy();
                }
            });

            windowLayout = new VLayout();
            windowLayout.setSize("100%", "100%");
            windowLayout.setAlign(Alignment.CENTER);
            windowLayout.setMembersMargin(5);
            addItem(windowLayout);

            show();
        }

        void showTrace(final Integer executionId) {
            windowLayout.removeMembers(windowLayout.getMembers());
            final Label loading = new Label(ClientConfig.messages.loading() + "...");
            loading.setAlign(Alignment.CENTER);
            loading.setIcon("loading.gif");
            loading.setIconSize(16);
            windowLayout.addMember(loading);

            StringBuilder dataBuilder = new StringBuilder();
            dataBuilder.append("action=queryExecutionTrace");
            dataBuilder.append("&executionId=").append(executionId);

            final String baseUrl = GWT.getModuleBaseURL();
            String servicePath = ClientConfig.constants.envTaskService();
            String requestUrl = baseUrl + servicePath.substring(servicePath.lastIndexOf('/') + 1);

            RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, requestUrl);
            try {
                builder.sendRequest(dataBuilder.toString(), new RequestCallback() {

                    @Override
                    public void onResponseReceived(Request request, Response response) {
                        if (response.getStatusCode() == Response.SC_OK) {
                            windowLayout.removeMember(loading);
                            HTMLFlow tracePane = new HTMLFlow();
                            tracePane.setWidth("99%");
                            tracePane.setHeight100();
                            tracePane.setContents("<pre>" + escapeHtml(response.getText()) + "</pre>");
                            HLayout wrapperLayout = new HLayout();
                            wrapperLayout.addMember(tracePane);
                            windowLayout.addMember(wrapperLayout);
                            HLayout controls = new HLayout();
                            controls.setSize("99%", "20");
                            controls.setMargin(5);
                            controls.setMembersMargin(5);
                            controls.setAlign(Alignment.CENTER);
                            IButton refreshButton = new IButton(ClientConfig.messages.refresh());
                            refreshButton.addClickHandler(new ClickHandler() {
                                @Override
                                public void onClick(ClickEvent event) {
                                    TraceWindow.this.showTrace(executionId);
                                }
                            });
                            controls.addMember(refreshButton);
                            windowLayout.addMember(controls);
                        } else {
                            SC.warn(response.getStatusCode() + " - " + response.getStatusText());
                        }
                    }

                    @Override
                    public void onError(Request request, Throwable exception) {
                        SC.warn(exception.getMessage());
                    }

                });
            } catch (Exception e) {
                SC.warn(e.getMessage());
            }
        }

        private String escapeHtml(Object value) {
            return value.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
