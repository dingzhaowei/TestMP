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

package org.testmp.datastore.client;

import java.io.File;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

public class DataStoreClient {

    private static Logger log = Logger.getLogger(DataStoreClient.class.getSimpleName());

    // private HttpClient httpClient = new DefaultHttpClient();

    private URI dataStoreURL;

    /**
     * Construct a client to specified data store
     * 
     * @param url
     *            the data store location
     */
    public DataStoreClient(URI url) {
        this.dataStoreURL = url;
    }

    public DataStoreClient(String url) {
        this.dataStoreURL = URI.create(url);
    }

    public String getDataStoreURL() {
        return dataStoreURL.toString();
    }

    /**
     * Count the total data in the store
     * 
     * @return
     * @throws DataStoreClientException
     */
    public int countTotalData() throws DataStoreClientException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(dataStoreURL);
        try {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("action", "count"));
            params.add(new BasicNameValuePair("type", "data"));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
            httpPost.setEntity(entity);
            HttpResponse resp = httpClient.execute(httpPost);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String count = EntityUtils.toString(resp.getEntity(), "UTF-8");
                return Integer.parseInt(count);
            } else {
                StatusLine status = resp.getStatusLine();
                log.severe(status.getStatusCode() + ": " + status.getReasonPhrase());
                return -1;
            }
        } catch (Exception e) {
            throw new DataStoreClientException("Failed to count data", e);
        } finally {
            httpPost.releaseConnection();
        }
    }

    /**
     * Get wrapped tagged data of specified type by specified tags and
     * properties
     * 
     * @param type
     * @param tags
     * @param properties
     * @return
     * @throws DataStoreClientException
     */
    public <T> List<DataInfo<T>> getData(Class<T> type, String[] tags, Map<String, Object> properties)
            throws DataStoreClientException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(dataStoreURL);
        try {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("action", "get"));
            params.add(new BasicNameValuePair("type", "data"));
            if (tags != null && tags.length > 0) {
                String tagListParam = this.convertListToRequestParam(Arrays.asList(tags));
                params.add(new BasicNameValuePair("tag", tagListParam));
            }
            if (properties != null && !properties.isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                params.add(new BasicNameValuePair("property", mapper.writeValueAsString(properties)));
            }
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
            httpPost.setEntity(entity);
            HttpResponse resp = httpClient.execute(httpPost);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String json = EntityUtils.toString(resp.getEntity(), "UTF-8");
                return convertJsonToDataInfoList(json, type);
            } else {
                StatusLine status = resp.getStatusLine();
                log.severe(status.getStatusCode() + ": " + status.getReasonPhrase());
                return new LinkedList<DataInfo<T>>();
            }
        } catch (Exception e) {
            throw new DataStoreClientException("Failed to get data", e);
        } finally {
            httpPost.releaseConnection();
        }
    }

    /**
     * Find the data id with specified tags and properties
     * 
     * @param tags
     * @param properties
     * @return
     * @throws DataStoreClientException
     */
    public List<Integer> findData(String[] tags, Map<String, Object> properties) throws DataStoreClientException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(dataStoreURL);
        try {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("action", "find"));
            params.add(new BasicNameValuePair("type", "data"));
            if (tags != null && tags.length > 0) {
                String tagListParam = this.convertListToRequestParam(Arrays.asList(tags));
                params.add(new BasicNameValuePair("tag", tagListParam));
            }
            if (properties != null && !properties.isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                params.add(new BasicNameValuePair("property", mapper.writeValueAsString(properties)));
            }
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
            httpPost.setEntity(entity);
            HttpResponse resp = httpClient.execute(httpPost);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String json = EntityUtils.toString(resp.getEntity(), "UTF-8");
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(json, new TypeReference<List<Integer>>() {
                });
            } else {
                StatusLine status = resp.getStatusLine();
                log.severe(status.getStatusCode() + ": " + status.getReasonPhrase());
                return new LinkedList<Integer>();
            }
        } catch (Exception e) {
            throw new DataStoreClientException("Failed to find data", e);
        } finally {
            httpPost.releaseConnection();
        }
    }

    /**
     * Get wrapped tagged data of specified type by specified tags
     * 
     * @param type
     * @param tags
     * @return
     * @throws DataStoreClientException
     */
    public <T> List<DataInfo<T>> getDataByTag(Class<T> type, String... tags) throws DataStoreClientException {
        return getData(type, tags, null);
    }

    /**
     * Find the data id with specified tags
     * 
     * @param tags
     * @return
     * @throws DataStoreClientException
     */
    public List<Integer> findDataByTag(String... tags) throws DataStoreClientException {
        return findData(tags, null);
    }

    /**
     * Get wrapped tagged data of specified type by specified properties
     * 
     * @param <T>
     * @param type
     * @param properties
     * @return
     * @throws DataStoreClientException
     */
    public <T> List<DataInfo<T>> getDataByProperty(Class<T> type, Map<String, Object> properties)
            throws DataStoreClientException {
        return getData(type, null, properties);
    }

    /**
     * Find the data id with specified properties
     * 
     * @param properties
     * @return
     * @throws DataStoreClientException
     */
    public List<Integer> findDataByProperty(Map<String, Object> properties) throws DataStoreClientException {
        return findData(null, properties);
    }

    /**
     * Get wrapped tagged data of specified type by id
     * 
     * @param type
     * @param id
     * @return
     * @throws DataStoreClientException
     */
    public <T> DataInfo<T> getDataById(Class<T> type, int id) throws DataStoreClientException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(dataStoreURL);
        try {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("action", "get"));
            params.add(new BasicNameValuePair("type", "data"));
            params.add(new BasicNameValuePair("id", "" + id));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
            httpPost.setEntity(entity);
            HttpResponse resp = httpClient.execute(httpPost);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String json = EntityUtils.toString(resp.getEntity(), "UTF-8");
                List<DataInfo<T>> result = convertJsonToDataInfoList(json, type);
                return result.isEmpty() ? null : result.get(0);
            } else {
                StatusLine status = resp.getStatusLine();
                log.severe(status.getStatusCode() + ": " + status.getReasonPhrase());
                return null;
            }
        } catch (Exception e) {
            throw new DataStoreClientException("Failed to get data by id", e);
        } finally {
            httpPost.releaseConnection();
        }
    }

    /**
     * Get wrapped tagged data of specified id range
     * 
     * @param type
     * @param startId
     * @param endId
     * @return
     * @throws DataStoreClientException
     */
    public <T> List<DataInfo<T>> getDataByRange(Class<T> type, int startId, int endId) throws DataStoreClientException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(dataStoreURL);
        try {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("action", "get"));
            params.add(new BasicNameValuePair("type", "data"));
            params.add(new BasicNameValuePair("range", startId + "," + endId));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
            httpPost.setEntity(entity);
            HttpResponse resp = httpClient.execute(httpPost);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String json = EntityUtils.toString(resp.getEntity(), "UTF-8");
                List<DataInfo<T>> result = convertJsonToDataInfoList(json, type);
                return result;
            } else {
                StatusLine status = resp.getStatusLine();
                log.severe(status.getStatusCode() + ": " + status.getReasonPhrase());
                return new LinkedList<DataInfo<T>>();
            }
        } catch (Exception e) {
            throw new DataStoreClientException("Failed to get data by range", e);
        } finally {
            httpPost.releaseConnection();
        }
    }

    /**
     * Merge several data into single new data. The properties of the previous
     * specified data takes precedence than the followings
     * 
     * @param type
     * @param id
     * @param anotherIds
     * @return
     * @throws DataStoreClientException
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <T> DataInfo<T> mergeData(Class<T> type, int id, int... anotherIds) throws DataStoreClientException {
        DataInfo<Map> dataInfo = getDataById(Map.class, id);
        List<String> tags = dataInfo.getTags();
        Map data = dataInfo.getData();
        for (int anotherId : anotherIds) {
            DataInfo<Map> anotherDataInfo = getDataById(Map.class, anotherId);
            for (String tag : anotherDataInfo.getTags()) {
                if (!tags.contains(tag)) {
                    tags.add(tag);
                }
            }
            for (Object entry : anotherDataInfo.getData().entrySet()) {
                Object key = ((Map.Entry) entry).getKey();
                Object value = ((Map.Entry) entry).getValue();
                if (!data.containsKey(key)) {
                    data.put(key, value);
                }
            }

        }
        dataInfo.setTags(tags);
        dataInfo.setData(data);
        ObjectMapper mapper = new ObjectMapper();
        try {
            return DataInfo.valueOf(mapper.writeValueAsString(dataInfo), type);
        } catch (Exception e) {
            throw new DataStoreClientException(e);
        }
    }

    /**
     * Add one or more wrapped tagged data
     * 
     * @param someDataInfo
     * @return
     * @throws DataStoreClientException
     */
    @SuppressWarnings("unchecked")
    public List<Integer> addData(DataInfo<?>... someDataInfo) throws DataStoreClientException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(dataStoreURL);
        try {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("action", "add"));
            params.add(new BasicNameValuePair("type", "data"));
            params.add(new BasicNameValuePair("content", converDataInfoListToJson(Arrays.asList(someDataInfo))));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
            httpPost.setEntity(entity);
            HttpResponse resp = httpClient.execute(httpPost);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String json = EntityUtils.toString(resp.getEntity(), "UTF-8");
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(json, List.class);
            } else {
                StatusLine status = resp.getStatusLine();
                log.severe(status.getStatusCode() + ": " + status.getReasonPhrase());
                return new LinkedList<Integer>();
            }
        } catch (Exception e) {
            throw new DataStoreClientException("Failed to add data", e);
        } finally {
            httpPost.releaseConnection();
        }
    }

    /**
     * Delete one or more tagged data by id
     * 
     * @param ids
     * @return
     * @throws DataStoreClientException
     */
    public boolean deleteData(Integer... ids) throws DataStoreClientException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(dataStoreURL);
        try {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("action", "delete"));
            params.add(new BasicNameValuePair("type", "data"));
            String idListParam = convertListToRequestParam(Arrays.asList(ids));
            params.add(new BasicNameValuePair("id", idListParam));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
            httpPost.setEntity(entity);
            HttpResponse resp = httpClient.execute(httpPost);
            return resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (Exception e) {
            throw new DataStoreClientException("Failed to delete data", e);
        } finally {
            httpPost.releaseConnection();
        }
    }

    /**
     * Get all the stored tags
     * 
     * @return
     * @throws DataStoreClientException
     */
    public List<Tag> getTags() throws DataStoreClientException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(dataStoreURL);
        try {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("action", "get"));
            params.add(new BasicNameValuePair("type", "tag"));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
            httpPost.setEntity(entity);
            HttpResponse resp = httpClient.execute(httpPost);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String json = EntityUtils.toString(resp.getEntity(), "UTF-8");
                return convertJsonToObjectList(json, Tag.class);
            } else {
                StatusLine status = resp.getStatusLine();
                log.severe(status.getStatusCode() + ": " + status.getReasonPhrase());
                return new LinkedList<Tag>();
            }
        } catch (Exception e) {
            throw new DataStoreClientException("Failed to get tags", e);
        } finally {
            httpPost.releaseConnection();
        }
    }

    /**
     * Add a tag to the data of specified id
     * 
     * @param dataId
     * @param tag
     * @return
     * @throws DataStoreClientException
     */
    public boolean addTagToData(int dataId, String tag) throws DataStoreClientException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(dataStoreURL);
        try {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("action", "add"));
            params.add(new BasicNameValuePair("type", "tag"));
            params.add(new BasicNameValuePair("id", "" + dataId));
            params.add(new BasicNameValuePair("tag", tag));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
            httpPost.setEntity(entity);
            HttpResponse resp = httpClient.execute(httpPost);
            return resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (Exception e) {
            throw new DataStoreClientException("Failed to add tag", e);
        } finally {
            httpPost.releaseConnection();
        }
    }

    /**
     * Delete a tag from the data of specified id
     * 
     * @param dataId
     * @param tag
     * @return
     * @throws DataStoreClientException
     */
    public boolean deleteTagFromData(int dataId, String tag) throws DataStoreClientException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(dataStoreURL);
        try {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("action", "delete"));
            params.add(new BasicNameValuePair("type", "tag"));
            params.add(new BasicNameValuePair("id", "" + dataId));
            params.add(new BasicNameValuePair("tag", tag));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
            httpPost.setEntity(entity);
            HttpResponse resp = httpClient.execute(httpPost);
            return resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (Exception e) {
            throw new DataStoreClientException("Failed to delete tag", e);
        } finally {
            httpPost.releaseConnection();
        }
    }

    /**
     * Get all the stored string property values mapping to the specified key
     * 
     * @param key
     * @param tags
     *            - limit the range of properties by specifying related tags
     * @return
     * @throws DataStoreClientException
     */
    public List<String> getPropertyValues(String key, String... tags) throws DataStoreClientException {
        return getPropertyValues(String.class, key, tags);
    }

    /**
     * Get all the stored property values mapping to the specified key
     * 
     * @param type
     * @param key
     * @param tags
     *            - limit the range of properties by specifying related tags
     * @return
     * @throws DataStoreClientException
     */
    public <T> List<T> getPropertyValues(Class<T> type, String key, String... tags) throws DataStoreClientException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(dataStoreURL);
        try {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("action", "get"));
            params.add(new BasicNameValuePair("type", "property"));
            params.add(new BasicNameValuePair("key", key));
            params.add(new BasicNameValuePair("tag", convertListToRequestParam(Arrays.asList(tags))));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
            httpPost.setEntity(entity);
            HttpResponse resp = httpClient.execute(httpPost);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String json = EntityUtils.toString(resp.getEntity(), "UTF-8");
                List<T> result = convertJsonToObjectList(json, type);
                return result;
            } else {
                StatusLine status = resp.getStatusLine();
                log.severe(status.getStatusCode() + ": " + status.getReasonPhrase());
                return new LinkedList<T>();
            }
        } catch (Exception e) {
            throw new DataStoreClientException("Failed to get data by range", e);
        } finally {
            httpPost.releaseConnection();
        }
    }

    /**
     * Add a property to the data of specified id
     * 
     * @param dataId
     * @param key
     * @param value
     * @return
     * @throws DataStoreClientException
     */
    public boolean addPropertyToData(int dataId, String key, Object value) throws DataStoreClientException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(dataStoreURL);
        try {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("action", "add"));
            params.add(new BasicNameValuePair("type", "property"));
            params.add(new BasicNameValuePair("id", "" + dataId));

            Map<String, Object> property = new HashMap<String, Object>();
            property.put("key", key);
            property.put("value", value);
            ObjectMapper mapper = new ObjectMapper();
            params.add(new BasicNameValuePair("property", mapper.writeValueAsString(property)));

            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
            httpPost.setEntity(entity);
            HttpResponse resp = httpClient.execute(httpPost);
            return resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (Exception e) {
            throw new DataStoreClientException("Failed to add property", e);
        } finally {
            httpPost.releaseConnection();
        }
    }

    /**
     * Delete a property from the data of specified id
     * 
     * @param dataId
     * @param key
     * @return
     * @throws DataStoreClientException
     */
    public boolean deletePropertyFromData(int dataId, String key) throws DataStoreClientException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(dataStoreURL);
        try {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("action", "delete"));
            params.add(new BasicNameValuePair("type", "property"));
            params.add(new BasicNameValuePair("id", "" + dataId));
            params.add(new BasicNameValuePair("key", key));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
            httpPost.setEntity(entity);
            HttpResponse resp = httpClient.execute(httpPost);
            return resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (Exception e) {
            throw new DataStoreClientException("Failed to delete property", e);
        } finally {
            httpPost.releaseConnection();
        }
    }

    /**
     * Save data in data store into specified file with JSON representation
     * 
     * @param filepath
     * @throws DataStoreClientException
     */
    public void saveDataToFile(String filepath) throws DataStoreClientException {
        List<Tag> tags = getTags();
        HashSet<Integer> idSet = new HashSet<Integer>();
        for (Tag tag : tags) {
            idSet.addAll(tag.getRelatedDataIds());
        }
        LinkedList<Integer> idList = new LinkedList<Integer>(idSet);
        Collections.sort(idList);
        int maxDataToGetEachTime = 500, i = 0;
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(filepath, "UTF-8");
            writer.print("[");
            boolean isFirst = true;
            while (i < idList.size()) {
                int startId = idList.get(i);
                i = i + maxDataToGetEachTime - 1;
                int endId = (i >= idList.size() ? idList.getLast() : idList.get(i));
                i++;
                List<DataInfo<Object>> dataInfoList = getDataByRange(Object.class, startId, endId);
                for (DataInfo<Object> dataInfo : dataInfoList) {
                    if (!isFirst) {
                        writer.println(",");
                    } else {
                        isFirst = false;
                    }
                    writer.print(dataInfo.toString());
                }
            }
            writer.println("]");
        } catch (Exception e) {
            throw new DataStoreClientException("Failed to save data to file", e);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Upload data from specified file of JSON representation to data store
     * 
     * @param filepath
     * @return the id list of uploaded data
     * @throws DataStoreClientException
     */
    public List<Integer> uploadDataFromFile(String filepath) throws DataStoreClientException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<DataInfo<Object>> dataInfoList = mapper.readValue(new File(filepath),
                    new TypeReference<List<DataInfo<Object>>>() {
                    });
            return addData(dataInfoList.toArray(new DataInfo[0]));
        } catch (Exception e) {
            throw new DataStoreClientException("Failed to upload data from file", e);
        }
    }

    /**
     * Get the meta info of data of specified id
     * 
     * @param dataIds
     * @return
     * @throws DataStoreClientException
     */
    public List<MetaInfo> getMetaInfo(Integer... dataIds) throws DataStoreClientException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(dataStoreURL);
        try {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("action", "get"));
            params.add(new BasicNameValuePair("type", "meta"));
            String idListParam = convertListToRequestParam(Arrays.asList(dataIds));
            params.add(new BasicNameValuePair("id", idListParam));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
            httpPost.setEntity(entity);
            HttpResponse resp = httpClient.execute(httpPost);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String json = EntityUtils.toString(resp.getEntity(), "UTF-8");
                List<MetaInfo> result = convertJsonToObjectList(json, MetaInfo.class);
                return result;
            } else {
                StatusLine status = resp.getStatusLine();
                log.severe(status.getStatusCode() + ": " + status.getReasonPhrase());
                return new LinkedList<MetaInfo>();
            }
        } catch (Exception e) {
            throw new DataStoreClientException("Failed to get meta data", e);
        } finally {
            httpPost.releaseConnection();
        }
    }

    /**
     * Update the meta info of data of specified id
     * 
     * @param key
     * @param value
     * @param dataIds
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unused")
    private void updateMetaInfo(String key, Object value, Integer... dataIds) throws Exception {
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(dataStoreURL);
        try {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("type", "meta"));
            String idListParam = convertListToRequestParam(Arrays.asList(dataIds));
            params.add(new BasicNameValuePair("id", idListParam));
            params.add(new BasicNameValuePair("key", key));
            if (value != null) {
                params.add(new BasicNameValuePair("action", "add"));
                ObjectMapper mapper = new ObjectMapper();
                params.add(new BasicNameValuePair("value", mapper.writeValueAsString(value)));
            } else {
                params.add(new BasicNameValuePair("action", "delete"));
            }
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
            httpPost.setEntity(entity);
            HttpResponse resp = httpClient.execute(httpPost);
            if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                log.warning("Failed to update meta info [" + key + ":" + value + "] for " + dataIds);
            }
        } finally {
            httpPost.releaseConnection();
        }
    }

    @SuppressWarnings("unused")
    private URIBuilder getCustomURIBuilder() {
        URIBuilder builder = new URIBuilder();
        builder.setScheme(dataStoreURL.getScheme());
        builder.setHost(dataStoreURL.getHost());
        builder.setPort(dataStoreURL.getPort());
        builder.setPath(dataStoreURL.getPath());
        return builder;
    }

    private <T> String convertListToRequestParam(List<T> list) {
        StringBuilder sb = new StringBuilder();
        for (T elem : list) {
            sb.append(elem.toString()).append(",");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    private String converDataInfoListToJson(List<DataInfo<?>> dataInfoList) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean isFirst = true;
        for (DataInfo<?> dataInfo : dataInfoList) {
            if (!isFirst) {
                sb.append(",");
            } else {
                isFirst = false;
            }
            sb.append(dataInfo.toString());
        }
        sb.append("]");
        return sb.toString();
    }

    private <T> List<DataInfo<T>> convertJsonToDataInfoList(String s, Class<T> type) {
        List<DataInfo<T>> dataInfoList = new LinkedList<DataInfo<T>>();
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode dataInfoListNode = mapper.readTree(s);
            for (int i = 0; i < dataInfoListNode.size(); i++) {
                JsonNode dataInfoNode = dataInfoListNode.get(i);
                dataInfoList.add(DataInfo.valueOf(dataInfoNode.toString(), type));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return dataInfoList;
    }

    private <T> List<T> convertJsonToObjectList(String s, Class<T> type) {
        List<T> objList = new LinkedList<T>();
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode objListNode = mapper.readTree(s);
            for (int i = 0; i < objListNode.size(); i++) {
                JsonNode objNode = objListNode.get(i);
                objList.add(mapper.readValue(objNode.toString(), type));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return objList;
    }
}
