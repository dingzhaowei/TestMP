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

import java.util.List;

import org.testmp.webconsole.shared.ClientConfig;
import org.testmp.webconsole.shared.ClientUtils;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.json.client.JSONObject;
import com.smartgwt.client.data.AdvancedCriteria;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.FilterBuilder;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

public class FilterWindow extends Window {

    private DataSource filterSource;

    public enum FilterType {
        TEST_CASE, TEST_DATA;
    }

    public FilterWindow(final FilterType filterType, final ListGrid listGrid, DataSource filterSource) {
        this.filterSource = filterSource;

        setWidth(700);
        setHeight(300);

        switch (filterType) {
        case TEST_CASE:
            setTitle(ClientConfig.messages.testCaseFilter());
            break;
        case TEST_DATA:
            setTitle(ClientConfig.messages.testDataFilter());
            break;
        }

        ClientUtils.unifySimpleWindowStyle(this);
        setShowMaximizeButton(true);
        setCanDragResize(true);

        VLayout layout = new VLayout();
        ClientUtils.unifyWindowLayoutStyle(layout);
        addItem(layout);

        VLayout filterLayout = new VLayout();
        filterLayout.setWidth("99%");
        filterLayout.setMargin(5);
        layout.addMember(filterLayout);

        final FilterBuilder filterBuilder = new FilterBuilder();
        filterBuilder.setDataSource(listGrid.getDataSource());
        filterBuilder.setLayoutAlign(Alignment.CENTER);
        filterBuilder.setAutoWidth();
        filterBuilder.setOverflow(Overflow.VISIBLE);
        filterBuilder.setCriteria(ClientConfig.getCurrentFilterCriteria(filterType));
        filterLayout.addMember(filterBuilder);

        HLayout controls = new HLayout();
        ClientUtils.unifyControlsLayoutStyle(controls);
        layout.addMember(controls);

        IButton okButton = new IButton(ClientConfig.messages.ok());
        okButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                AdvancedCriteria criteria = filterBuilder.getCriteria();
                FilterWindow.this.destroy();
                ClientConfig.setCurrentFilterCriteria(criteria, filterType);
                listGrid.filterData(criteria);
            }

        });
        controls.addMember(okButton);

        IButton saveButton = new IButton(ClientConfig.messages.save());
        saveButton.addClickHandler(new ClickHandler() {

            @SuppressWarnings("unchecked")
            @Override
            public void onClick(ClickEvent event) {
                AdvancedCriteria criteria = filterBuilder.getCriteria();
                List<Object> c = (List<Object>) criteria.getValues().get("criteria");
                if (c != null && c.size() > 0) {
                    Window window = new SaveFilterWindow(criteria);
                    window.show();
                } else {
                    SC.say(ClientConfig.messages.nullFilter());
                }
            }

        });
        controls.addMember(saveButton);

        IButton loadButton = new IButton(ClientConfig.messages.load());
        loadButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                Window window = new LoadFilterWindow(filterBuilder);
                window.show();
            }

        });
        controls.addMember(loadButton);

        IButton resetButton = new IButton(ClientConfig.messages.reset());
        resetButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                filterBuilder.setCriteria(new AdvancedCriteria());
            }

        });
        controls.addMember(resetButton);

        if (ClientConfig.currentUser == null) {
            saveButton.setDisabled(true);
            loadButton.setDisabled(true);
        }
    }

    private class SaveFilterWindow extends Window {

        String criteriaJson;

        SaveFilterWindow(AdvancedCriteria criteria) {
            criteriaJson = new JSONObject(criteria.getJsObj()).toString();
            setWidth(450);
            setHeight(100);
            setTitle(ClientConfig.messages.save());
            ClientUtils.unifySimpleWindowStyle(this);

            VLayout layout = new VLayout();
            ClientUtils.unifyWindowLayoutStyle(layout);
            addItem(layout);

            final DynamicForm form = new DynamicForm();
            form.setWidth100();
            form.setNumCols(4);
            ComboBoxItem filterNameItem = new ComboBoxItem("filterName");
            filterNameItem.setTitle(ClientConfig.messages.filterName());
            filterNameItem.setOptionDataSource(filterSource);
            filterNameItem.setRequired(true);
            CheckboxItem isDefaultItem = new CheckboxItem("isDefault");
            isDefaultItem.setTitle(ClientConfig.messages.isDefault());
            form.setFields(filterNameItem, isDefaultItem);
            layout.addMember(form);

            HLayout controls = new HLayout();
            ClientUtils.unifyControlsLayoutStyle(controls);
            layout.addMember(controls);

            IButton okButton = new IButton(ClientConfig.messages.ok());
            okButton.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    if (form.validate()) {
                        String filterName = form.getValueAsString("filterName").trim();
                        String isDefault = form.getValueAsString("isDefault");
                        SaveFilterWindow.this.destroy();
                        Record record = new Record();
                        record.setAttribute("userName", ClientConfig.currentUser);
                        record.setAttribute("filterName", filterName);
                        record.setAttribute("criteria", criteriaJson);
                        record.setAttribute("isDefault", isDefault == null ? false : isDefault);
                        filterSource.addData(record);
                    }
                }

            });
            controls.addMember(okButton);
        }

    }

    private class LoadFilterWindow extends Window {

        FilterBuilder filterBuilder;

        LoadFilterWindow(FilterBuilder fb) {
            this.filterBuilder = fb;
            setWidth(450);
            setHeight(100);
            setTitle(ClientConfig.messages.load());
            ClientUtils.unifySimpleWindowStyle(this);

            VLayout layout = new VLayout();
            ClientUtils.unifyWindowLayoutStyle(layout);
            addItem(layout);

            final DynamicForm form = new DynamicForm();
            form.setWidth100();
            form.setNumCols(4);
            ComboBoxItem filterNameItem = new ComboBoxItem("filterName");
            filterNameItem.setTitle(ClientConfig.messages.filterName());
            filterNameItem.setOptionDataSource(filterSource);
            filterNameItem.setRequired(true);
            CheckboxItem toRemoveItem = new CheckboxItem("toRemove");
            toRemoveItem.setTitle(ClientConfig.messages.remove());
            form.setFields(filterNameItem, toRemoveItem);
            layout.addMember(form);

            HLayout controls = new HLayout();
            ClientUtils.unifyControlsLayoutStyle(controls);
            layout.addMember(controls);

            IButton okButton = new IButton(ClientConfig.messages.ok());
            okButton.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    if (form.validate()) {
                        final String filterName = form.getValueAsString("filterName").trim();
                        final String toRemove = form.getValueAsString("toRemove");
                        LoadFilterWindow.this.destroy();

                        Criteria criteria = new Criteria("filterName", filterName);
                        filterSource.fetchData(criteria, new DSCallback() {

                            @Override
                            public void execute(DSResponse response, Object rawData, DSRequest request) {
                                if (rawData.toString().isEmpty()) {
                                    SC.say(ClientConfig.messages.nullFilter());
                                } else {
                                    JavaScriptObject jsonObj = JsonUtils.safeEval(rawData.toString());
                                    filterBuilder.setCriteria(new AdvancedCriteria(jsonObj));
                                    if (toRemove != null && toRemove.equalsIgnoreCase("true")) {
                                        Record record = new Record();
                                        record.setAttribute("userName", ClientConfig.currentUser);
                                        record.setAttribute("filterName", filterName);
                                        filterSource.removeData(record);
                                    }
                                }
                            }

                        });
                    }
                }

            });
            controls.addMember(okButton);
        }

    }
}
