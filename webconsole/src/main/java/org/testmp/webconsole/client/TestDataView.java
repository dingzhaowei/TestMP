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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.testmp.webconsole.shared.WebConsoleClientConfig;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.OperationBinding;
import com.smartgwt.client.data.Record;
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
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.SummaryFunction;
import com.smartgwt.client.widgets.grid.events.FilterEditorSubmitEvent;
import com.smartgwt.client.widgets.grid.events.FilterEditorSubmitHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

public class TestDataView extends VLayout {

    private DataSource testDataSource;

    private ListGrid testDataGrid;

    @Override
    protected void onInit() {
        super.onInit();

        testDataSource = new TestDataSource();

        testDataGrid = new ListGrid();
        testDataGrid.setWidth("99%");
        testDataGrid.setLayoutAlign(Alignment.CENTER);
        testDataGrid.setDataSource(testDataSource);
        testDataGrid.setAutoFetchData(true);
        testDataGrid.setShowAllRecords(true);
        testDataGrid.setDataFetchMode(FetchMode.LOCAL);
        testDataGrid.setFixedRecordHeights(false);
        testDataGrid.setCanRemoveRecords(true);
        testDataGrid.setCanEdit(true);
        testDataGrid.setShowRollOver(false);
        testDataGrid.setShowGridSummary(true);
        testDataGrid.setShowFilterEditor(true);
        testDataGrid.addFilterEditorSubmitHandler(new FilterEditorSubmitHandler() {

            @SuppressWarnings("rawtypes")
            @Override
            public void onFilterEditorSubmit(FilterEditorSubmitEvent event) {
                Criteria criteria = event.getCriteria();
                Map values = criteria.getValues();
                Criteria convertedCriteria = new Criteria();
                for (Object entry : values.entrySet()) {
                    String key = ((Map.Entry) entry).getKey().toString();
                    String value = ((Map.Entry) entry).getValue().toString();
                    if (key.equals("tags")) {
                        List<String> tags = Arrays.asList(value.split("\\s*,\\s*"));
                        Collections.sort(tags);
                        StringBuilder sb = new StringBuilder();
                        for (String tag : tags) {
                            if (sb.length() > 0) {
                                sb.append(',');
                            }
                            sb.append(tag);
                        }
                        value = sb.toString();
                    } else if (key.equals("id")) {
                        value = value.trim();
                    }
                    convertedCriteria.addCriteria(new Criteria(key, value));
                }
                testDataGrid.filterData(convertedCriteria);
                event.cancel();
            }

        });

        ListGridField idField = new ListGridField("id", "Id", 50);
        idField.setAlign(Alignment.LEFT);
        idField.setCanEdit(false);
        idField.setShowGridSummary(false);

        ListGridField tagsField = new ListGridField("tags", "Tags", 200);
        tagsField.setShowHover(true);
        tagsField.setSummaryFunction(new SummaryFunction() {

            @Override
            public Object getSummaryValue(Record[] records, ListGridField field) {
                return "Total Data: " + records.length;
            }

        });

        ListGridField propertiesField = new ListGridField("properties", "Properties");
        propertiesField.setShowHover(true);
        propertiesField.setHoverCustomizer(new HoverCustomizer() {

            @Override
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                boolean isSummary = record.getIsGridSummary() != null && record.getIsGridSummary().booleanValue();
                if (value == null || value.toString().isEmpty() || isSummary) {
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

        ListGridField createTimeField = new ListGridField("createTime", "Create Time", 150);
        createTimeField.setType(ListGridFieldType.DATE);
        createTimeField.setCanEdit(false);
        createTimeField.setCanFilter(false);

        ListGridField lastModifyTimeField = new ListGridField("lastModifyTime", "Last Modify Time", 150);
        lastModifyTimeField.setType(ListGridFieldType.DATE);
        lastModifyTimeField.setCanEdit(false);
        lastModifyTimeField.setCanFilter(false);

        testDataGrid.setFields(idField, tagsField, propertiesField, createTimeField, lastModifyTimeField);

        addMember(testDataGrid);

        HLayout controls = new HLayout();
        controls.setSize("99%", "20");
        controls.setMargin(10);
        controls.setMembersMargin(5);
        controls.setLayoutAlign(Alignment.CENTER);
        controls.setAlign(Alignment.RIGHT);

        IButton newDataButton = new IButton("New");
        newDataButton.setIcon("newdata.png");
        newDataButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                NewDataWindow window = new NewDataWindow();
                window.show();
            }

        });
        controls.addMember(newDataButton);

        IButton reloadButton = new IButton("Reload");
        reloadButton.setIcon("reload.png");
        reloadButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                testDataGrid.invalidateCache();
            }

        });
        controls.addMember(reloadButton);

        addMember(controls);
    }

    private class NewDataWindow extends Window {

        NewDataWindow() {
            setWidth(400);
            setHeight(250);
            setTitle("New Test Data");
            setShowMinimizeButton(false);
            setIsModal(true);
            setShowModalMask(true);
            centerInPage();
            addCloseClickHandler(new CloseClickHandler() {
                public void onCloseClick(CloseClickEvent event) {
                    NewDataWindow.this.destroy();
                }
            });

            VLayout layout = new VLayout();
            layout.setSize("100%", "100%");
            layout.setMargin(5);
            layout.setMembersMargin(5);
            layout.setAlign(Alignment.CENTER);
            addItem(layout);

            final DynamicForm form = new DynamicForm();
            final TextItem tagsItem = new TextItem("tags", "Tags");
            tagsItem.setWidth(300);
            tagsItem.setRequired(true);
            final TextAreaItem propsItem = new TextAreaItem("properties", "Properties");
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

            IButton okButton = new IButton("OK");
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

            IButton cancelButton = new IButton("Cancel");
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
            String servicePath = WebConsoleClientConfig.constants.testDataService();
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
            idField.setRequired(true);

            DataSourceTextField tagsField = new DataSourceTextField("tags", "Tags");
            DataSourceTextField propertiesField = new DataSourceTextField("properties", "Properties", Integer.MAX_VALUE);
            DataSourceTextField createTimeField = new DataSourceTextField("createTime", "Create Time");
            DataSourceTextField lastModifyTimeField = new DataSourceTextField("lastModifyTime", "Last Modify Time");

            setFields(idField, tagsField, propertiesField, createTimeField, lastModifyTimeField);
        }
    }

    private String escapeHtml(Object value) {
        return value.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
