package org.testmp.webconsole.client;

import java.util.HashMap;
import java.util.Map;

import org.testmp.webconsole.shared.ClientConfig;
import org.testmp.webconsole.shared.ClientUtils;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.fields.DataSourceBooleanField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

public class TeamView extends VLayout {

    private Map<String, DataSource> dataSources;

    private ListGrid teamGrid;

    @Override
    protected void onInit() {
        super.onInit();

        prepareDataSources();

        teamGrid = new ListGrid() {

            @Override
            protected Canvas getExpansionComponent(final ListGridRecord record) {
                VLayout layout = new VLayout(5);
                layout.setPadding(5);

                final ListGrid memberGrid = new ListGrid();
                memberGrid.setWidth("95%");
                memberGrid.setHeight(224);
                memberGrid.setLayoutAlign(Alignment.CENTER);
                memberGrid.setShowRollOver(false);
                memberGrid.setCanRemoveRecords(true);
                memberGrid.setWarnOnRemoval(true);
                memberGrid.setCanEdit(true);
                memberGrid.setDataSource(dataSources.get("teamMemberDS"));

                ListGridField memberIdField = new ListGridField("id");
                memberIdField.setHidden(true);

                ListGridField memberNameField = new ListGridField("name", ClientConfig.messages.member());
                memberNameField.setRequired(true);
                memberNameField.setWidth(50);

                ListGridField memberFullNameField = new ListGridField("fullName", ClientConfig.messages.fullName());
                memberFullNameField.setWidth(50);

                ListGridField memberEmailField = new ListGridField("email", ClientConfig.messages.email());
                memberEmailField.setWidth(50);

                ListGridField memberIsAdminField = new ListGridField("isAdmin", ClientConfig.messages.admin());
                memberIsAdminField.setType(ListGridFieldType.BOOLEAN);

                memberGrid.setFields(memberIdField, memberNameField, memberFullNameField, memberEmailField,
                        memberIsAdminField);
                layout.addMember(memberGrid);

                Criteria criteria = new Criteria();
                criteria.setAttribute("team", record.getAttribute("name"));
                memberGrid.fetchData(criteria);

                HLayout controls = new HLayout();
                ClientUtils.unifyControlsLayoutStyle(controls);
                layout.addMember(controls);

                IButton newMemberButton = new IButton(ClientConfig.messages.new_());
                newMemberButton.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        memberGrid.startEditingNew();
                    }
                });
                controls.addMember(newMemberButton);

                return layout;
            }
        };

        teamGrid.setWidth("99%");
        teamGrid.setLayoutAlign(Alignment.CENTER);
        teamGrid.setShowRollOver(false);
        teamGrid.setShowRecordComponents(true);
        teamGrid.setShowRecordComponentsByCell(true);
        teamGrid.setShowFilterEditor(true);
        teamGrid.setCanRemoveRecords(true);
        teamGrid.setWarnOnRemoval(true);
        teamGrid.setCanEdit(true);
        teamGrid.setCanExpandRecords(true);
        teamGrid.setAutoFetchData(true);
        teamGrid.setDataSource(dataSources.get("teamInfoDS"));

        ListGridField teamIdField = new ListGridField("id");
        teamIdField.setHidden(true);

        ListGridField teamNameField = new ListGridField("name", ClientConfig.messages.team());
        teamNameField.setRequired(true);

        ListGridField teamEmailField = new ListGridField("email", ClientConfig.messages.email());
        teamEmailField.setCanFilter(false);

        teamGrid.setFields(teamIdField, teamNameField, teamEmailField);
        addMember(teamGrid);

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

        IButton newEnvButton = new IButton(ClientConfig.messages.new_());
        newEnvButton.setIcon("newenv.png");
        newEnvButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                teamGrid.startEditingNew();
            }

        });
        primaryControls.addMember(newEnvButton);
    }

    private void prepareDataSources() {
        dataSources = new HashMap<String, DataSource>();

        DataSource teamInfoSource = ClientUtils.createDataSource("teamInfoDS", ClientConfig.constants.userService());
        DataSourceIntegerField teamIdField = new DataSourceIntegerField("id");
        teamIdField.setHidden(true);
        teamIdField.setPrimaryKey(true);
        DataSourceTextField teamNameField = new DataSourceTextField("name");
        teamNameField.setRequired(true);
        DataSourceTextField teamEmailField = new DataSourceTextField("email");
        teamInfoSource.setFields(teamIdField, teamNameField, teamEmailField);
        dataSources.put("teamInfoDS", teamInfoSource);

        DataSource teamMemberSource = ClientUtils
                .createDataSource("teamMemberDS", ClientConfig.constants.userService());
        DataSourceIntegerField memberIdField = new DataSourceIntegerField("id");
        memberIdField.setHidden(true);
        memberIdField.setPrimaryKey(true);
        DataSourceTextField memberNameField = new DataSourceTextField("name");
        memberNameField.setRequired(true);
        DataSourceTextField memberFullNameField = new DataSourceTextField("fullName");
        DataSourceTextField memberEmailField = new DataSourceTextField("email");
        DataSourceBooleanField memberisAdminField = new DataSourceBooleanField("isAdmin");
        teamMemberSource.setFields(memberIdField, memberNameField, memberFullNameField, memberEmailField,
                memberisAdminField);
        dataSources.put("teamMemberDS", teamMemberSource);
    }

}
