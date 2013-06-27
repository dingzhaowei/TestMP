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

package org.testmp.datastore.server;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.testmp.datastore.model.Data;
import org.testmp.datastore.model.Property;

import com.sleepycat.bind.serial.SerialSerialKeyCreator;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.je.SecondaryDatabase;

public class DataStore {

    private static final String CLASS_CATALOG = "datastore_class_catalog";

    public static final String DATA_STORE = "data_store";

    public static final String TAG_STORE = "tag_store";

    public static final String PROPERTY_STORE = "property_store";

    public static final String META_INFO_STORE = "meta_info_store";

    public static final String DATA_STORE_BY_PROPERTIES = "data_store_by_properties";

    public static final String PROPERTY_STORE_BY_KEY = "property_store_by_key";

    public static final String PROPERTY_STORE_BY_KEY_VALUE = "property_store_by_key_value";

    private Environment env;

    private StoredClassCatalog classCatalog;

    private Database dataStore;

    private Database tagStore;

    private Database propertyStore;

    private Database metaInfoStore;

    private SecondaryDatabase dataStoreByProperties;

    private SecondaryDatabase propertyStoreByKey;

    private SecondaryDatabase propertyStoreByKeyValue;

    public DataStore(String homeDirectory) throws DatabaseException {
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setTransactional(true);
        envConfig.setAllowCreate(true);
        env = new Environment(new File(homeDirectory), envConfig);

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(true);
        dbConfig.setAllowCreate(true);

        Database catalogDb = env.openDatabase(null, CLASS_CATALOG, dbConfig);
        classCatalog = new StoredClassCatalog(catalogDb);
        dataStore = env.openDatabase(null, DATA_STORE, dbConfig);
        tagStore = env.openDatabase(null, TAG_STORE, dbConfig);
        propertyStore = env.openDatabase(null, PROPERTY_STORE, dbConfig);
        metaInfoStore = env.openDatabase(null, META_INFO_STORE, dbConfig);

        SecondaryConfig secConfig = new SecondaryConfig();
        secConfig.setTransactional(true);
        secConfig.setAllowCreate(true);
        secConfig.setSortedDuplicates(true);

        secConfig.setKeyCreator(new SerialSerialKeyCreator(classCatalog, Integer.class, Data.class, String.class) {

            @Override
            public Object createSecondaryKey(Object primaryKey, Object o) {
                List<Integer> relatedPropertyIds = new LinkedList<Integer>(((Data) o).getRelatedPropertyIds());
                Collections.sort(relatedPropertyIds);
                StringBuilder sb = new StringBuilder();
                for (int id : relatedPropertyIds) {
                    sb.append(id);
                }
                return sb.toString();
            }

        });
        dataStoreByProperties = env.openSecondaryDatabase(null, DATA_STORE_BY_PROPERTIES, dataStore, secConfig);

        secConfig.setKeyCreator(new SerialSerialKeyCreator(classCatalog, Integer.class, Data.class, String.class) {

            @Override
            public Object createSecondaryKey(Object primaryKey, Object o) {
                Property property = (Property) o;
                return property.getKey();
            }

        });
        propertyStoreByKey = env.openSecondaryDatabase(null, PROPERTY_STORE_BY_KEY, propertyStore, secConfig);

        secConfig.setKeyCreator(new SerialSerialKeyCreator(classCatalog, Integer.class, Data.class, String.class) {

            @Override
            public Object createSecondaryKey(Object primaryKey, Object o) {
                Property property = (Property) o;
                String key = property.getKey();
                Object value = property.getValue();
                ObjectMapper mapper = new ObjectMapper();
                try {
                    return key + mapper.writeValueAsString(value);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

        });
        propertyStoreByKeyValue = env
                .openSecondaryDatabase(null, PROPERTY_STORE_BY_KEY_VALUE, propertyStore, secConfig);
    }

    public Environment getEnv() {
        return env;
    }

    public StoredClassCatalog getClassCatalog() {
        return classCatalog;
    }

    public Database getDataStore() {
        return dataStore;
    }

    public Database getTagStore() {
        return tagStore;
    }

    public Database getPropertyStore() {
        return propertyStore;
    }

    public Database getMetaInfoStore() {
        return metaInfoStore;
    }

    public SecondaryDatabase getDataStoreByProperties() {
        return dataStoreByProperties;
    }

    public SecondaryDatabase getPropertyStoreByKey() {
        return propertyStoreByKey;
    }

    public SecondaryDatabase getPropertyStoreByKeyValue() {
        return propertyStoreByKeyValue;
    }

    public void close() throws DatabaseException {
        if (propertyStoreByKeyValue != null) {
            propertyStoreByKeyValue.close();
        }

        if (propertyStoreByKey != null) {
            propertyStoreByKey.close();
        }

        if (dataStoreByProperties != null) {
            dataStoreByProperties.close();
        }

        if (metaInfoStore != null) {
            metaInfoStore.close();
        }

        if (propertyStore != null) {
            propertyStore.close();
        }

        if (tagStore != null) {
            tagStore.close();
        }

        if (dataStore != null) {
            dataStore.close();
        }
        if (classCatalog != null) {
            classCatalog.close();
        }
        if (env != null) {
            env.close();
        }
    }
}
