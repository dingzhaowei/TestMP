package org.testmp.datastore.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class DataInfoTest {

    public static class Pair {
        private String key;

        private String value;

        public Pair() {
        }

        public Pair(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public boolean equals(Object o) {
            return o instanceof Pair && ((Pair) o).key.equals(key) && ((Pair) o).value.equals(value);
        }

    }

    public static class Data {
        private String p1;

        private int p2;

        private Map<String, String> p3;

        private Pair p4;

        public Data() {
        }

        public String getP1() {
            return p1;
        }

        public void setP1(String p1) {
            this.p1 = p1;
        }

        public int getP2() {
            return p2;
        }

        public void setP2(int p2) {
            this.p2 = p2;
        }

        public Map<String, String> getP3() {
            return p3;
        }

        public void setP3(Map<String, String> p3) {
            this.p3 = p3;
        }

        public Pair getP4() {
            return p4;
        }

        public void setP4(Pair p4) {
            this.p4 = p4;
        }

    }

    public static class AnotherData {

        private String anotherP1;

        private Pair anotherP2;

        public String getAnotherP1() {
            return anotherP1;
        }

        public void setAnotherP1(String anotherP1) {
            this.anotherP1 = anotherP1;
        }

        public Pair getAnotherP2() {
            return anotherP2;
        }

        public void setAnotherP2(Pair anotherP2) {
            this.anotherP2 = anotherP2;
        }
    }

    public static class MergedData {
        private String p1;

        private int p2;

        private Map<String, String> p3;

        private Pair p4;

        private String anotherP1;

        private Pair anotherP2;

        public String getP1() {
            return p1;
        }

        public int getP2() {
            return p2;
        }

        public Map<String, String> getP3() {
            return p3;
        }

        public Pair getP4() {
            return p4;
        }

        public String getAnotherP1() {
            return anotherP1;
        }

        public Pair getAnotherP2() {
            return anotherP2;
        }
    }

    @Test
    public void testDataInfo1() {
        DataInfo<Data> dataInfo = new DataInfo<Data>();
        Data data = new Data();
        data.setP1("hahaha");
        data.setP2(2345);
        HashMap<String, String> m = new HashMap<String, String>();
        m.put("rrr", "yyy");
        data.setP3(m);
        data.setP4(new Pair("key", "value"));
        dataInfo.setId(1);
        dataInfo.setTags(Arrays.asList(new String[] { "t1", "t2", "t3" }));
        dataInfo.setData(data);
        String s = dataInfo.toString();
        System.out.println(s);
        dataInfo = DataInfo.valueOf(s, Data.class);
        data = (Data) dataInfo.getData();
        System.out.println(data.getP3().get("rrr"));
        System.out.println(data.getP4().getValue());
    }

}
