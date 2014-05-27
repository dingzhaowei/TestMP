package org.testmp.webconsole.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.testmp.datastore.client.DataInfo;
import org.testmp.datastore.client.MetaInfo;
import org.testmp.sync.TestData;

public class TestDataAssemblyStrategy implements DataAssemblyStrategy {

    @Override
    public Map<String, Object> assemble(DataInfo<? extends Object> dataInfo, MetaInfo metaInfo) {
        TestData td = (TestData) dataInfo.getData();
        Map<String, Object> m = new HashMap<String, Object>();
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

        m.put("id", dataInfo.getId().toString());
        m.put("name", td.getName());
        m.put("parent", td.getParent());
        List<String> tags = dataInfo.getTags();
        Collections.sort(tags);
        StringBuilder sb = new StringBuilder();
        for (String tag : tags) {
            if (tag.equals("TestData")) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(tag);
        }
        m.put("tags", sb.toString());
        try {
            m.put("properties", writer.writeValueAsString(td.getProperties()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        m.put("createTime", metaInfo.getMetaInfo().get("create_time"));
        m.put("lastModifyTime", metaInfo.getMetaInfo().get("last_modify_time"));

        return m;
    }

}
