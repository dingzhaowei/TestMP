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
import java.util.LinkedList;
import java.util.Map;

import org.testmp.webconsole.client.FilterWindow.FilterType;
import org.testmp.webconsole.shared.ClientConfig;
import org.testmp.webconsole.shared.ClientUtils;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import com.smartgwt.client.data.AdvancedCriteria;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.Criterion;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.fields.DataSourceBooleanField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.FetchMode;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.OperatorId;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

public class TestDataView extends VLayout {

    private Map<String, DataSource> dataSources;

    private ListGrid testDataGrid;

    private IButton goBackButton;

    private LinkedList<Criteria> criteriaStack = new LinkedList<Criteria>();

    private HoverCustomizer hoverCustomizer = ClientUtils.createHoverCustomizer();

    @Override
    protected void onDraw() {
        if (ClientConfig.currentUser == null) {
            testDataGrid.fetchData();
        } else {
            Criteria criteria = new Criteria("isDefault", "true");
            dataSources.get("testDataFilterDS").fetchData(criteria, new DSCallback() {

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
        prepareDataSources();

        testDataGrid = new ListGrid();
        testDataGrid.setWidth("99%");
        testDataGrid.setLayoutAlign(Alignment.CENTER);
        testDataGrid.setDataSource(dataSources.get("testDataDS"));
        testDataGrid.setDataFetchMode(FetchMode.PAGED);
        testDataGrid.setFixedRecordHeights(false);
        testDataGrid.setCanRemoveRecords(true);
        testDataGrid.setWarnOnRemoval(true);
        testDataGrid.setCanEdit(true);
        testDataGrid.setShowRollOver(false);
        testDataGrid.setShowRecordComponents(true);
        testDataGrid.setShowRecordComponentsByCell(true);

        ListGridField nameField = new ListGridField("name", ClientConfig.messages.name(), 120);
        nameField.setCanEdit(false);
        nameField.setShowHover(true);
        nameField.setHoverCustomizer(hoverCustomizer);

        ListGridField parentField = new ListGridField("parent", ClientConfig.messages.parent(), 120);
        parentField.setShowHover(true);
        parentField.setHoverCustomizer(hoverCustomizer);
        parentField.addRecordClickHandler(new RecordClickHandler() {

            @Override
            public void onRecordClick(RecordClickEvent event) {
                final String parent = event.getRecord().getAttribute("parent");
                if (parent == null) {
                    return;
                }
                SC.confirm(ClientConfig.messages.goToParentData(), new BooleanCallback() {

                    @Override
                    public void execute(Boolean value) {
                        if (value == false) {
                            return;
                        }
                        AdvancedCriteria criteria = ClientConfig.getCurrentFilterCriteria(FilterType.TEST_DATA);
                        if (criteria.getCriteria() != null && criteria.getCriteria().length != 0) {
                            criteriaStack.addFirst(criteria);
                        } else {
                            criteriaStack.addFirst(new AdvancedCriteria());
                        }
                        criteria = new AdvancedCriteria();
                        criteria.setOperator(OperatorId.AND);
                        criteria.addCriteria(new Criterion("name", OperatorId.EQUALS, parent));
                        ClientConfig.setCurrentFilterCriteria(criteria, FilterType.TEST_DATA);
                        testDataGrid.filterData(criteria);
                        goBackButton.setDisabled(false);
                    }

                });
                event.cancel();
            }

        });

        ListGridField tagsField = new ListGridField("tags", ClientConfig.messages.tags(), 150);
        tagsField.setShowHover(true);
        tagsField.setHoverCustomizer(hoverCustomizer);

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

        testDataGrid
                .setFields(nameField, parentField, tagsField, propertiesField, createTimeField, lastModifyTimeField);

        addMember(testDataGrid);

        HLayout controls = new HLayout();
        ClientUtils.unifyControlsLayoutStyle(controls);
        addMember(controls);

        HLayout additionalControls = new HLayout();
        additionalControls.setMembersMargin(5);
        controls.addMember(additionalControls);

        HLayout primaryControls = new HLayout();
        primaryControls.setAlign(Alignment.RIGHT);
        primaryControls.setMembersMargin(5);
        controls.addMember(primaryControls);

        goBackButton = new IButton(ClientConfig.messages.back());
        goBackButton.setIcon("back.png");
        goBackButton.setDisabled(true);
        goBackButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                AdvancedCriteria criteria = (AdvancedCriteria) criteriaStack.poll();
                testDataGrid.filterData(criteria);
                ClientConfig.setCurrentFilterCriteria(criteria, FilterType.TEST_DATA);
                if (criteriaStack.isEmpty()) {
                    goBackButton.setDisabled(true);
                }
            }

        });
        additionalControls.addMember(goBackButton);

        IButton newDataButton = new IButton(ClientConfig.messages.new_());
        newDataButton.setIcon("newdata.png");
        newDataButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                NewDataWindow window = new NewDataWindow();
                window.show();
            }

        });
        primaryControls.addMember(newDataButton);

        IButton filterButton = new IButton(ClientConfig.messages.filter());
        filterButton.setIcon("filter.png");
        filterButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                DataSource ds = dataSources.get("testDataFilterDS");
                FilterWindow window = new FilterWindow(FilterType.TEST_DATA, testDataGrid, ds);
                window.show();
            }

        });
        primaryControls.addMember(filterButton);

        IButton reloadButton = new IButton(ClientConfig.messages.reload());
        reloadButton.setIcon("reload.png");
        reloadButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                testDataGrid.invalidateCache();
            }

        });
        primaryControls.addMember(reloadButton);
    }

    private void prepareDataSources() {
        dataSources = new HashMap<String, DataSource>();

        DataSource testDataSource = ClientUtils
                .createDataSource("testDataDS", ClientConfig.constants.testDataService());
        DataSourceIntegerField idField = new DataSourceIntegerField("id", "ID");
        idField.setPrimaryKey(true);
        idField.setHidden(true);
        DataSourceTextField nameField = new DataSourceTextField("name", ClientConfig.messages.name());
        nameField.setRequired(true);
        DataSourceTextField propertiesField = new DataSourceTextField("properties", ClientConfig.messages.properties(),
                Integer.MAX_VALUE);
        propertiesField.setRequired(true);
        DataSourceTextField parentField = new DataSourceTextField("parent", ClientConfig.messages.parent());
        DataSourceTextField tagsField = new DataSourceTextField("tags", ClientConfig.messages.tags());
        DataSourceTextField createTimeField = new DataSourceTextField("createTime", ClientConfig.messages.createTime());
        DataSourceTextField lastModifyTimeField = new DataSourceTextField("lastModifyTime",
                ClientConfig.messages.lastModifyTime());
        testDataSource.setFields(idField, nameField, parentField, tagsField, propertiesField, createTimeField,
                lastModifyTimeField);
        dataSources.put("testDataDS", testDataSource);

        DataSource testDataFilterSource = ClientUtils.createDataSource("testDataFilterDS",
                ClientConfig.constants.userService());
        DataSourceTextField userNameField = new DataSourceTextField("userName");
        userNameField.setPrimaryKey(true);
        DataSourceTextField filterNameField = new DataSourceTextField("filterName");
        filterNameField.setPrimaryKey(true);
        DataSourceTextField criteriaField = new DataSourceTextField("criteria");
        DataSourceBooleanField isDefaultField = new DataSourceBooleanField("isDefault");
        testDataFilterSource.setFields(userNameField, filterNameField, criteriaField, isDefaultField);
        dataSources.put("testDataFilterDS", testDataFilterSource);

        DataSource testDataNameSource = ClientUtils.createDataSource("testDataNameDS",
                ClientConfig.constants.testDataService());
        DataSourceTextField dataNameField = new DataSourceTextField("name");
        testDataNameSource.setFields(dataNameField);
        dataSources.put("testDataNameDS", testDataNameSource);
    }

    private class NewDataWindow extends Window {

        NewDataWindow() {
            setWidth(400);
            setHeight(255);
            setTitle(ClientConfig.messages.newTestData());
            ClientUtils.unifySimpleWindowStyle(this);

            VLayout layout = new VLayout();
            ClientUtils.unifyWindowLayoutStyle(layout);
            addItem(layout);

            final DynamicForm form = new DynamicForm();
            form.setWidth("99%");

            final TextItem nameItem = new TextItem("name", ClientConfig.messages.name());
            nameItem.setWidth(300);
            nameItem.setRequired(true);

            final SelectItem parentItem = new SelectItem("parent", ClientConfig.messages.parent());
            parentItem.setWidth(300);
            parentItem.setOptionDataSource(dataSources.get("testDataNameDS"));
            parentItem.setValueField("name");
            parentItem.setAllowEmptyValue(true);

            final TextItem tagsItem = new TextItem("tags", ClientConfig.messages.tags());
            tagsItem.setWidth(300);

            final TextAreaItem propsItem = new TextAreaItem("properties", ClientConfig.messages.properties());
            propsItem.setWidth(300);
            propsItem.setRequired(true);

            form.setItems(nameItem, propsItem, parentItem, tagsItem);
            layout.addMember(form);

            HLayout controls = new HLayout();
            ClientUtils.unifyControlsLayoutStyle(controls);
            layout.addMember(controls);

            IButton okButton = new IButton(ClientConfig.messages.ok());
            okButton.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    if (form.validate()) {
                        ListGridRecord record = new ListGridRecord();
                        record.setAttribute("name", nameItem.getValue());
                        record.setAttribute("parent", parentItem.getValue());
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

    private String escapeHtml(Object value) {
        return value.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
