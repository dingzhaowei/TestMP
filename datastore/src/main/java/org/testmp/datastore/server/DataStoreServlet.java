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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.testmp.datastore.model.DataInfo;

@SuppressWarnings("serial")
public class DataStoreServlet extends HttpServlet {

    private DataStoreManager manager;

    public void init() {
        manager = DataStoreManager.getInstance();
        if (!manager.isOpenned()) {
            String homeDir = System.getProperty("dbHome");
            if (homeDir == null) {
                homeDir = System.getProperty("user.home") + File.separator + "tmp" + File.separator + "db";
                new File(homeDir).mkdirs();
            }
            try {
                manager.open(homeDir);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        BufferedReader reader = req.getReader();
        StringBuilder sb = new StringBuilder();
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            sb.append(line + "\n");
        }
        String entity = sb.toString().trim();

        HashMap<String, String> params = new HashMap<String, String>();
        for (String entry : entity.split("&")) {
            String[] pair = entry.split("=");
            String key = pair[0];
            String value = pair.length >= 2 ? URLDecoder.decode(pair[1], "UTF-8") : null;
            params.put(key, value);
        }

        log("Received POST request: " + params);

        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter writer = resp.getWriter();

        String action = params.get("action");
        if (action.equals("count")) {
            String type = params.get("type");
            if (type.equals("data")) {
                writer.print(manager.countTotalData());
            }
        } else if (action.equals("get")) {
            String type = params.get("type");
            if (type.equals("data")) {
                // Get data by tags or properties
                if (params.containsKey("tag") || params.containsKey("property")) {
                    String tagsParam = params.get("tag");
                    List<String> tags = new ArrayList<String>();
                    if (tagsParam != null) {
                        tags = Arrays.asList(tagsParam.split(","));
                    }
                    String propertiesParam = params.get("property");
                    List<Integer> propertyIds = new ArrayList<Integer>();
                    if (propertiesParam != null) {
                        propertyIds = Utils.convertJsonToPropertyIdList(propertiesParam);
                    }
                    writer.print(Utils.converDataInfoListToJson(manager.getData(tags, propertyIds)));
                }
                // Get data by id
                else if (params.containsKey("id")) {
                    int id = Integer.parseInt(params.get("id"));
                    writer.print(Utils.converDataInfoListToJson(manager.getDataById(id)));
                }
                // Get data by range
                else if (params.containsKey("range")) {
                    String[] range = params.get("range").split(",");
                    int startId = Integer.parseInt(range[0]);
                    int endId = Integer.parseInt(range[1]);
                    writer.print(Utils.converDataInfoListToJson(manager.getDataByRange(startId, endId)));
                }
            }
            // Get tags
            else if (type.equals("tag")) {
                writer.print(Utils.convertTagListToJson(manager.getTags()));
            }
            // Get property values
            else if (type.equals("property")) {
                String key = params.get("key");
                List<String> tags = new ArrayList<String>();
                if (params.get("tag") != null) {
                    for (String tag : params.get("tag").split(",")) {
                        tags.add(tag);
                    }
                }
                writer.print(Utils.convertPropertyValueListToJson(manager.getPropertyValues(key, tags)));
            }
            // Get meta info
            else if (type.equals("meta")) {
                List<Integer> idList = new ArrayList<Integer>();
                for (String id : params.get("id").split(",")) {
                    idList.add(Integer.parseInt(id));
                }
                List<Object> result = new ArrayList<Object>(manager.getMetaInfo(idList));
                writer.print(Utils.convertPropertyValueListToJson(result));
            }
        } else if (action.equals("find")) {
            String type = params.get("type");
            if (type.equals("data")) {
                if (params.containsKey("tag") || params.containsKey("property")) {
                    String tagsParam = params.get("tag");
                    List<String> tags = new ArrayList<String>();
                    if (tagsParam != null) {
                        tags = Arrays.asList(tagsParam.split(","));
                    }
                    String propertiesParam = params.get("property");
                    List<Integer> propertyIds = new ArrayList<Integer>();
                    if (propertiesParam != null) {
                        propertyIds = Utils.convertJsonToPropertyIdList(propertiesParam);
                    }
                    List<Integer> result = new ArrayList<Integer>();
                    for (DataInfo dataInfo : manager.getData(tags, propertyIds)) {
                        result.add(dataInfo.getId());
                    }
                    ObjectMapper mapper = new ObjectMapper();
                    writer.print(mapper.writeValueAsString(result));
                }
            }
        } else if (action.equals("add")) {
            String type = params.get("type");
            if (type.equals("data")) {
                String content = params.get("content");
                writer.print(manager.addData(Utils.convertJsonToDataInfoList(content)).toString());
            } else if (type.equals("tag")) {
                int dataId = Integer.parseInt(params.get("id"));
                String tag = params.get("tag");
                manager.addTagToData(dataId, tag);
            } else if (type.equals("property")) {
                int dataId = Integer.parseInt(params.get("id"));
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> property = mapper.readValue(params.get("property"),
                        new TypeReference<Map<String, Object>>() {
                        });
                manager.addPropertyToData(dataId, property.get("key").toString(), property.get("value"));
            }
            // else if (type.equals("meta")) {
            // List<Integer> idList = new ArrayList<Integer>();
            // for (String dataId : params.get("id").split(",")) {
            // idList.add(Integer.parseInt(dataId));
            // }
            // String key = params.get("key");
            // Object value =
            // Utils.convertJsonToPropertyValue(params.get("value"));
            // manager.addMetaInfoToData(idList, key, value);
            // }
        } else if (action.equals("delete")) {
            String type = params.get("type");
            if (type.equals("data")) {
                List<Integer> idList = new ArrayList<Integer>();
                for (String id : params.get("id").split(",")) {
                    idList.add(Integer.parseInt(id));
                }
                manager.deleteData(idList);
            } else if (type.equals("tag")) {
                int dataId = Integer.parseInt(params.get("id"));
                String tag = params.get("tag");
                manager.deleteTagFromData(dataId, tag);
            } else if (type.equals("property")) {
                int dataId = Integer.parseInt(params.get("id"));
                String key = params.get("key");
                manager.deletePropertyFromData(dataId, key);
            }
            // else if (type.equals("meta")) {
            // List<Integer> idList = new ArrayList<Integer>();
            // for (String dataId : params.get("id").split(",")) {
            // idList.add(Integer.parseInt(dataId));
            // }
            // String key = params.get("key");
            // manager.deleteMetaInfoFromData(idList, key);
            // }
        }

        writer.flush();
        writer.close();
    }
}
