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

import java.util.HashMap;
import java.util.Map;

import org.testmp.webconsole.client.FilterWindow.FilterType;
import org.testmp.webconsole.client.ReportWindow.ReportType;
import org.testmp.webconsole.shared.ClientConfig;
import org.testmp.webconsole.shared.ClientUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.json.client.JSONObject;
import com.smartgwt.client.data.AdvancedCriteria;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.OperationBinding;
import com.smartgwt.client.data.RestDataSource;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSOperationType;
import com.smartgwt.client.types.DSProtocol;
import com.smartgwt.client.types.FetchMode;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

public class TestDataView extends VLayout {

    private DataSource testDataSource;

    private DataSource testDataFilterSource;

    private ListGrid testDataGrid;

    @Override
    protected void onDraw() {
        if (ClientConfig.currentUser == null) {
            testDataGrid.fetchData();
        } else {
            Criteria criteria = new Criteria("isDefault", "true");
            testDataFilterSource.fetchData(criteria, new DSCallback() {

                @Override
                public void execute(DSResponse response, Object rawData, DSRequest request) {
                    if (rawData.toString().isEmpty()) {
                        testDataGrid.fetchData();
                    } else {
                        JavaScriptObject jsonObj = JsonUtils.safeEval(rawData.toString());
                        AdvancedCriteria initialCriteria = new AdvancedCriteria(jsonObj);
                        ClientConfig.setCurrentFilterCriteria(initialCriteria, FilterType.TEST_DATA);
                        testDataGrid.fetchData(initialCriteria);
                    }
                }

            });
        }
        super.onDraw();
    }

    @Override
    protected void onInit() {
        super.onInit();

        testDataSource = new TestDataSource();

        testDataFilterSource = ClientUtils.createFilterSource("testDataFilterDS");

        testDataGrid = new ListGrid();
        testDataGrid.setWidth("99%");
        testDataGrid.setLayoutAlign(Alignment.CENTER);
        testDataGrid.setDataSource(testDataSource);
        testDataGrid.setDataFetchMode(FetchMode.PAGED);
        testDataGrid.setFixedRecordHeights(false);
        testDataGrid.setCanRemoveRecords(true);
        testDataGrid.setWarnOnRemoval(true);
        testDataGrid.setCanEdit(true);
        testDataGrid.setShowRollOver(false);

        ListGridField idField = new ListGridField("id", "ID", 50);
        idField.setAlign(Alignment.LEFT);
        idField.setCanEdit(false);

        ListGridField tagsField = new ListGridField("tags", ClientConfig.messages.tags(), 200);
        tagsField.setShowHover(true);

        ListGridField propertiesField = new ListGridField("properties", ClientConfig.messages.properties());
        propertiesField.setShowHover(true);
        propertiesField.setHoverCustomizer(new HoverCustomizer() {

            @Override
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                if (value == null || value.toString().isEmpty()) {
                    return null;
                }
                return "<pre>" + escapeHtml(value) + "</pre>";
            }

        });
        propertiesField.setCellFormatter(new CellFormatter() {

            @Override
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                return value == null ? null : escapeHtml(value);
            }

        });

        ListGridField createTimeField = new ListGridField("createTime", ClientConfig.messages.createTime(), 150);
        createTimeField.setType(ListGridFieldType.DATE);
        createTimeField.setCanEdit(false);

        ListGridField lastModifyTimeField = new ListGridField("lastModifyTime", ClientConfig.messages.lastModifyTime(),
                150);
        lastModifyTimeField.setType(ListGridFieldType.DATE);
        lastModifyTimeField.setCanEdit(false);

        testDataGrid.setFields(idField, tagsField, propertiesField, createTimeField, lastModifyTimeField);

        addMember(testDataGrid);

        HLayout controls = new HLayout();
        controls.setSize("99%", "20");
        controls.setMargin(10);
        controls.setMembersMargin(5);
        controls.setLayoutAlign(Alignment.CENTER);
        controls.setAlign(Alignment.RIGHT);

        IButton newDataButton = new IButton(ClientConfig.messages.new_());
        newDataButton.setIcon("newdata.png");
        newDataButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                NewDataWindow window = new NewDataWindow();
                window.show();
            }

        });
        controls.addMember(newDataButton);

        IButton filterButton = new IButton(ClientConfig.messages.filter());
        filterButton.setIcon("filter.png");
        filterButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                FilterWindow window = new FilterWindow(FilterType.TEST_DATA, testDataGrid, testDataFilterSource);
                window.show();
            }

        });
        controls.addMember(filterButton);

        IButton reloadButton = new IButton(ClientConfig.messages.reload());
        reloadButton.setIcon("reload.png");
        reloadButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                testDataGrid.invalidateCache();
            }

        });
        controls.addMember(reloadButton);

        IButton reportButton = new IButton(ClientConfig.messages.report());
        reportButton.setIcon("report.png");
        reportButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                Map<String, Object> params = new HashMap<String, Object>();
                params.put("action", "create");
                AdvancedCriteria criteria = ClientConfig.getCurrentFilterCriteria(FilterType.TEST_DATA);
                String criteriaJson = new JSONObject(criteria.getJsObj()).toString();
                params.put("testDataCriteria", criteriaJson);
                ReportWindow reportWindow = new ReportWindow();
                reportWindow.showReport(ReportType.DATA_ANALYTICS, params);
            }

        });
        controls.addMember(reportButton);

        addMember(controls);
    }

    private class NewDataWindow extends Window {

        NewDataWindow() {
            setWidth(400);
            setHeight(250);
            setTitle(ClientConfig.messages.newTestData());
            ClientUtils.unifySimpleWindowStyle(this);

            VLayout layout = new VLayout();
            layout.setSize("100%", "100%");
            layout.setMargin(5);
            layout.setMembersMargin(5);
            layout.setAlign(Alignment.CENTER);
            addItem(layout);

            final DynamicForm form = new DynamicForm();
            final TextItem tagsItem = new TextItem("tags", ClientConfig.messages.tags());
            tagsItem.setWidth(300);
            tagsItem.setRequired(true);
            final TextAreaItem propsItem = new TextAreaItem("properties", ClientConfig.messages.properties());
            propsItem.setWidth(300);
            propsItem.setRequired(true);
            form.setItems(tagsItem, propsItem);
            form.setWidth("99%");
            layout.addMember(form);

            HLayout controls = new HLayout();
            controls.setSize("99%", "20");
            controls.setMargin(5);
            controls.setMembersMargin(5);
            controls.setLayoutAlign(Alignment.CENTER);
            controls.setAlign(Alignment.CENTER);
            layout.addMember(controls);

            IButton okButton = new IButton(ClientConfig.messages.ok());
            okButton.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    if (form.validate()) {
                        ListGridRecord record = new ListGridRecord();
                        record.setAttribute("tags", tagsItem.getValue());
                        record.setAttribute("properties", propsItem.getValue());
                        testDataGrid.addData(record, new DSCallback() {

                            @Override
                            public void execute(DSResponse response, Object rawData, DSRequest request) {
                                if (response.getStatus() == DSResponse.STATUS_SUCCESS) {
                                    NewDataWindow.this.destroy();
                                }
                            }

                        });
                    }
                }

            });
            controls.addMember(okButton);

            IButton cancelButton = new IButton(ClientConfig.messages.cancel());
            cancelButton.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    NewDataWindow.this.destroy();
                }

            });
            controls.addMember(cancelButton);
        }

    }

    private class TestDataSource extends RestDataSource {

        TestDataSource() {
            setID("testDataDS");
            setDataFormat(DSDataFormat.JSON);
            setClientOnly(false);

            String baseUrl = GWT.getModuleBaseURL();
            String servicePath = ClientConfig.constants.testDataService();
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

            DataSourceIntegerField idField = new DataSourceIntegerField("id", "ID");
            idField.setPrimaryKey(true);

            DataSourceTextField tagsField = new DataSourceTextField("tags", ClientConfig.messages.tags());
            tagsField.setRequired(true);

            DataSourceTextField propertiesField = new DataSourceTextField("properties",
                    ClientConfig.messages.properties(), Integer.MAX_VALUE);
            propertiesField.setRequired(true);

            DataSourceTextField createTimeField = new DataSourceTextField("createTime",
                    ClientConfig.messages.createTime());
            createTimeField.setCanFilter(false);

            DataSourceTextField lastModifyTimeField = new DataSourceTextField("lastModifyTime",
                    ClientConfig.messages.lastModifyTime());
            lastModifyTimeField.setCanFilter(false);

            setFields(idField, tagsField, propertiesField, createTimeField, lastModifyTimeField);
        }
    }

    private String escapeHtml(Object value) {
        return value.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
