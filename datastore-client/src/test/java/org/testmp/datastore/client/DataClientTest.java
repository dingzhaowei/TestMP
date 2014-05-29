package org.testmp.datastore.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.testmp.datastore.client.DataInfoTest.AnotherData;
import org.testmp.datastore.client.DataInfoTest.Data;
import org.testmp.datastore.client.DataInfoTest.MergedData;
import org.testmp.datastore.client.DataInfoTest.Pair;

public class DataClientTest {

    private DataStoreClient client;

    private DataInfo<Data> dataInfo1, dataInfo2, dataInfo3;

    private DataInfo<AnotherData> dataInfo4;

    @Before
    public void setUp() {
        String dataStoreURI = System.getProperty("datastore.url");
        if (dataStoreURI == null) {
            return;
        }
        client = new DataStoreClient(dataStoreURI);

        Data dataA = new Data();
        dataA.setP1("aaaa");
        dataA.setP2(1234);
        HashMap<String, String> m1 = new HashMap<String, String>();
        m1.put("姓名", "丁兆伟");
        dataA.setP3(m1);
        dataA.setP4(new Pair("keyA", "valueA"));

        Data dataB = new Data();
        dataB.setP1("bbbb");
        dataB.setP2(4321);
        HashMap<String, String> m2 = new HashMap<String, String>();
        m2.put("姓名", "吴海毓");
        dataB.setP3(m2);
        dataB.setP4(new Pair("keyB", "valueB"));

        AnotherData anotherData = new AnotherData();
        anotherData.setAnotherP1("another");
        anotherData.setAnotherP2(new Pair("anotherKey", "anotherValue"));

        dataInfo1 = new DataInfo<Data>(null, Arrays.asList(new String[] { "标签1", "标签2" }), dataA);
        dataInfo2 = new DataInfo<Data>(null, Arrays.asList(new String[] { "标签2", "标签3" }), dataA);
        dataInfo3 = new DataInfo<Data>(null, Arrays.asList(new String[] { "标签4" }), dataB);
        dataInfo4 = new DataInfo<AnotherData>(null, Arrays.asList(new String[] { "anotherData" }), anotherData);
    }

    @Test
    public void testBasicDataOperations() throws Exception {
        if (client == null) {
            return;
        }

        List<Integer> ids = client.addData(dataInfo1, dataInfo2, dataInfo3, dataInfo4);
        System.out.println("Returned added data IDs: " + ids);
        Assert.assertEquals("Fail to add data", 3, ids.size());

        List<Tag> tags = client.getTags();
        System.out.println("Returned tags: " + tags);
        Assert.assertEquals("Fail to get tags", 5, tags.size());

        List<String> propertyValues = client.getPropertyValues("p1");
        System.out.println("Returned property values of p1: " + propertyValues);
        Assert.assertEquals("Fail to get property values", 2, propertyValues.size());

        @SuppressWarnings("rawtypes")
        List<Map> propertyValues2 = client.getPropertyValues(Map.class, "p3", "标签4");
        System.out.println("Returned property values of p3: " + propertyValues2);
        Assert.assertEquals("Fail to get property values", 1, propertyValues2.size());

        List<DataInfo<Data>> dataInfoList = client.getDataByTag(Data.class, "标签2");
        System.out.println("Returned data queried by tag: " + dataInfoList);
        Assert.assertEquals("Fail to get data by tag: ", 1, dataInfoList.size());

        List<Integer> dataIdList = client.findDataByTag("标签2");
        System.out.println("Returned data id queried by tag: " + dataIdList);
        Assert.assertEquals("Fail to find data by tag: ", 1, dataIdList.size());

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("p2", 4321);
        HashMap<String, String> m = new HashMap<String, String>();
        m.put("姓名", "吴海毓");
        props.put("p3", m);
        dataInfoList = client.getDataByProperty(Data.class, props);
        System.out.println("Returned data queried by property: " + dataInfoList);
        Assert.assertEquals("Fail to get data by property: ", 1, dataInfoList.size());

        dataIdList = client.findDataByProperty(props);
        System.out.println("Returned data id queried by property: " + dataIdList);
        Assert.assertEquals("Fail to find data by property: ", 1, dataIdList.size());

        props = new HashMap<String, Object>();
        props.put("p4", new Pair("keyA", "valueA"));
        dataInfoList = client.getData(Data.class, new String[] { "标签2" }, props);
        System.out.println("Returned data queried by tag&property: " + dataInfoList);
        Assert.assertEquals("Fail to get data by tag&prop: ", 1, dataInfoList.size());

        DataInfo<Data> dataInfo = client.getDataById(Data.class, 2);
        System.out.println("Returned data queried by id: " + dataInfo);
        Assert.assertNotNull("Fail to get data by id: ", dataInfo);

        dataInfoList = client.getDataByRange(Data.class, 1, 2);
        System.out.println(dataInfoList);
        Assert.assertEquals(2, dataInfoList.size());

        DataInfo<MergedData> mergedDataInfo = client.mergeData(MergedData.class, 1, 2, 3);
        System.out.println("Merged data from 1, 2 and 3: " + mergedDataInfo);
        Assert.assertEquals(5, mergedDataInfo.getTags().size());
        MergedData mergedData = mergedDataInfo.getData();
        Assert.assertEquals(dataInfo1.getData().getP1(), mergedData.getP1());
        Assert.assertEquals(dataInfo1.getData().getP2(), mergedData.getP2());
        Assert.assertEquals(dataInfo1.getData().getP3(), mergedData.getP3());
        Assert.assertEquals(dataInfo1.getData().getP4(), mergedData.getP4());
        Assert.assertEquals(dataInfo4.getData().getAnotherP1(), mergedData.getAnotherP1());
        Assert.assertEquals(dataInfo4.getData().getAnotherP2(), mergedData.getAnotherP2());

        List<MetaInfo> metaInfoList = client.getMetaInfo(1, 2, 3);
        System.out.println("Returned meta info list: " + metaInfoList);

        client.saveDataToFile("/tmp/temp.txt");

        Assert.assertTrue("Fail to delete 1st data", client.deleteData(1));
        System.out.println("Remained tags after delete dataA: " + client.getTags());
        Assert.assertTrue("Fail to delete 2nd data", client.deleteData(2));
        System.out.println("Remained tags after delete dataB: " + client.getTags());

        client.uploadDataFromFile("/tmp/temp.txt");
        Assert.assertEquals(3, client.countTotalData());

        Assert.assertTrue(client.deleteData(1, 2, 3));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testAdditionalDataOperations() throws Exception {
        if (client == null) {
            return;
        }

        String addedTag = "标签+";
        int id = client.addData(dataInfo1).get(0);
        Assert.assertTrue(client.addTagToData(id, addedTag));
        DataInfo<Data> dataInfo = client.getDataById(Data.class, id);
        Assert.assertTrue(dataInfo.getTags().contains(addedTag));
        List<Tag> tags = client.getTags();
        boolean existed = false;
        for (Tag tag : tags) {
            if (tag.getName().equals(addedTag)) {
                Assert.assertTrue(tag.getRelatedDataIds().contains(id));
                existed = true;
                break;
            }
        }
        Assert.assertTrue(existed);

        Assert.assertTrue(client.deleteTagFromData(id, addedTag));
        dataInfo = client.getDataById(Data.class, id);
        Assert.assertTrue(!dataInfo.getTags().contains(addedTag));
        tags = client.getTags();
        existed = false;
        for (Tag tag : tags) {
            if (tag.getName().equals(addedTag)) {
                existed = true;
                break;
            }
        }
        Assert.assertFalse(existed);

        String key = "关键字";
        Object value = new Integer[] { 19, 82 };
        Assert.assertTrue(client.addPropertyToData(id, key, new Integer[] { 19, 82 }));
        Map p = new HashMap();
        p.put(key, value);
        List<DataInfo<Map>> dataInfoList = client.getDataByProperty(Map.class, p);
        Assert.assertTrue(!p.isEmpty());
        Map map = dataInfoList.get(0).getData();
        Assert.assertTrue(map.containsKey(key));
        System.out.println(map.get(key));

        Assert.assertTrue(client.deletePropertyFromData(id, key));
        dataInfoList = client.getDataByProperty(Map.class, p);
        Assert.assertTrue(dataInfoList.isEmpty());
        dataInfo = client.getDataById(Data.class, id);
        Assert.assertTrue(dataInfo != null);
    }

}
