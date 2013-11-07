package org.testmp.webconsole.server;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.testmp.datastore.client.DataInfo;
import org.testmp.datastore.client.MetaInfo;
import org.testmp.sync.TestCase;
import org.testmp.sync.TestCase.RunRecord;

public class TestCaseAssemblyStrategy implements DataAssemblyStrategy {

    @Override
    public Map<String, Object> assemble(DataInfo<? extends Object> dataInfo, MetaInfo metaInfo) {
        TestCase tc = (TestCase) dataInfo.getData();
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("id", dataInfo.getId().toString());
        m.put("project", tc.getProject());
        m.put("name", tc.getName());
        m.put("description", tc.getDescription());
        m.put("automation", tc.getAutomation());
        m.put("robustness", String.format("%.3f", tc.evaluateRobustness()));
        m.put("robustnessTrend", tc.getRobustnessTrend() + ".png");
        m.put("avgTestTime", String.format("%.1f", tc.evaluateAverageTestTime()));
        m.put("timeVolatility", String.format("%.3f", tc.evaluateTimeVolatility()));

        List<String> tags = dataInfo.getTags();
        StringBuilder sb = new StringBuilder();
        for (String tag : tags) {
            if (tag.equals("TestCase")) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(tag);
        }
        m.put("tags", sb.toString());
        m.put("createTime", metaInfo.getMetaInfo().get("create_time"));
        m.put("lastModifyTime", metaInfo.getMetaInfo().get("last_modify_time"));

        List<RunRecord> runRecordList = tc.getRunHistory();
        ObjectMapper mapper = new ObjectMapper();
        try {
            m.put("runHistory", mapper.writeValueAsString(runRecordList));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (!runRecordList.isEmpty()) {
            RunRecord record = runRecordList.get(runRecordList.size() - 1);
            Date lastRunTime = new Date(record.getRecordTime());
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            m.put("lastRunTime", format.format(lastRunTime));
        }

        return m;
    }

}
