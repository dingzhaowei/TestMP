package org.testmp.webconsole;

import java.util.Arrays;

import org.junit.Test;
import org.testmp.datastore.client.DataInfo;
import org.testmp.datastore.client.DataStoreClient;
import org.testmp.webconsole.model.User;

public class AddUsers {

    @Test
    public void addUsers() throws Exception {
        String url = "http://localhost:10081/DataStore.do";
        DataStoreClient client = new DataStoreClient(url);

        DataInfo<User> dataInfo1 = new DataInfo<User>();
        dataInfo1.setTags(Arrays.asList("User"));
        dataInfo1.setData(new User("admin", "123"));

        DataInfo<User> dataInfo2 = new DataInfo<User>();
        dataInfo2.setTags(Arrays.asList("User"));
        dataInfo2.setData(new User("leodzw", "123"));

        DataInfo<User> dataInfo3 = new DataInfo<User>();
        dataInfo3.setTags(Arrays.asList("User"));
        dataInfo3.setData(new User("dingzw", "123"));

        System.out.println(client.addData(dataInfo1, dataInfo2, dataInfo3));
    }

}
