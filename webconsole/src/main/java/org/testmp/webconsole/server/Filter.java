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

package org.testmp.webconsole.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.math.NumberUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

public class Filter {

    public static final String OPERATOR_AND = "and";

    public static final String OPERATOR_OR = "or";

    public static final String OPERATOR_NOT = "not";

    public static final String OPERATOR_EQUALS_DISREGARD_CASE = "iEquals";

    public static final String OPERATOR_NOT_EQUAL_DISREGARD_CASE = "iNotEqual";

    public static final String OPERATOR_BETWEEN_INCLUSIVE = "iBetweenInclusive";

    public static final String OPERATOR_CONTAINS = "iContains";

    public static final String OPERATOR_STARTS_WITH = "iStartsWith";

    public static final String OPERATOR_ENDS_WITH = "iEndsWith";

    public static final String OPERATOR_DOES_NOT_CONTAIN = "iNotContains";

    public static final String OPERATOR_DOES_NOT_START_WITH = "iNotStartsWith";

    public static final String OPERATOR_DOES_NOT_END_WITH = "iNotEndsWith";

    public static final String OPERATOR_EQUALS = "equals";

    public static final String OPERATOR_NOT_EQUAL = "notEqual";

    public static final String OPERATOR_LESS_THAN = "lessThan";

    public static final String OPERATOR_GREATER_THAN = "greaterThan";

    public static final String OPERATOR_LESS_THAN_OR_EQUAL_TO = "lessOrEqual";

    public static final String OPERATOR_GREATER_THAN_OR_EQUAL_TO = "greaterOrEqual";

    public static final String OPERATOR_BETWEEN_INCLUSIVE_MATCH_CASE = "betweenInclusive";

    public static final String OPERATOR_IS_NULL = "isNull";

    public static final String OPERATOR_IS_NOT_NULL = "notNull";

    public static final String OPERATOR_MATCHES_OTHER_FIELD = "equalsField";

    public static final String OPERATOR_DIFFERS_FROM_FIELD = "notEqualField";

    public static final String OPERATOR_MATCHES_OTHER_FIELD_CASE_INCENSITIVE = "iEqualsField";

    public static final String OPERATOR_DIFFERS_FROM_FIELD_CASE_INCENSITIVE = "iNotEqualField";

    public static final String OPERATOR_LESS_THAN_FIELD = "greaterThanField";

    public static final String OPERATOR_GREATER_THAN_FIELD = "lessThanField";

    public static final String OPERATOR_LESS_THAN_OR_EQUAL_TO_FIELD = "greaterOrEqualField";

    public static final String OPERATOR_GREATER_THAN_OR_EQUAL_TO_FIELD = "lessOrEqualField";

    private Criteria criteria;

    public Filter(Criteria criteria) {
        this.criteria = criteria;
    }

    public List<Map<String, Object>> doFilter(List<Map<String, Object>> originDataList) {
        List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> data : originDataList) {
            if (evaluate(data, criteria)) {
                dataList.add(data);
            }
        }
        return dataList;
    }

    private static boolean evaluate(Map<String, Object> data, Criteria criteria) {
        String operator = criteria.getOperator();
        if (operator.equals(OPERATOR_AND)) {
            for (Criteria sc : criteria.getSubCriteria()) {
                if (!evaluate(data, sc)) {
                    return false;
                }
            }
            return true;
        } else if (operator.equals(OPERATOR_OR)) {
            for (Criteria sc : criteria.getSubCriteria()) {
                if (evaluate(data, sc)) {
                    return true;
                }
            }
            return false;
        } else if (operator.equals(OPERATOR_NOT)) {
            for (Criteria sc : criteria.getSubCriteria()) {
                if (evaluate(data, sc)) {
                    return false;
                }
            }
            return true;
        } else if (operator.equals(OPERATOR_EQUALS_DISREGARD_CASE)) {
            String fieldName = criteria.getFieldName();
            String value = criteria.getValue();
            String value1 = (String) data.get(fieldName);
            return equalTo(value1, value, true);
        } else if (operator.equals(OPERATOR_NOT_EQUAL_DISREGARD_CASE)) {
            String fieldName = criteria.getFieldName();
            String value = criteria.getValue();
            String value1 = (String) data.get(fieldName);
            return !equalTo(value1, value, true);
        } else if (operator.equals(OPERATOR_BETWEEN_INCLUSIVE)) {
            String fieldName = criteria.getFieldName();
            String start = criteria.getStart();
            String end = criteria.getEnd();
            String value1 = (String) data.get(fieldName);
            return greaterThanOrEqualTo(value1, start, true) && lessThanOrEqualTo(value1, end, true);
        } else if (operator.equals(OPERATOR_CONTAINS)) {
            String fieldName = criteria.getFieldName();
            String value = criteria.getValue();
            String value1 = (String) data.get(fieldName);
            return value1 != null && value1.contains(value);
        } else if (operator.equals(OPERATOR_STARTS_WITH)) {
            String fieldName = criteria.getFieldName();
            String value = criteria.getValue();
            String value1 = (String) data.get(fieldName);
            return value1 != null && value1.startsWith(value);
        } else if (operator.equals(OPERATOR_ENDS_WITH)) {
            String fieldName = criteria.getFieldName();
            String value = criteria.getValue();
            String value1 = (String) data.get(fieldName);
            return value1 != null && value1.endsWith(value);
        } else if (operator.equals(OPERATOR_DOES_NOT_CONTAIN)) {
            String fieldName = criteria.getFieldName();
            String value = criteria.getValue();
            String value1 = (String) data.get(fieldName);
            return value1 != null && !value1.contains(value);
        } else if (operator.equals(OPERATOR_DOES_NOT_START_WITH)) {
            String fieldName = criteria.getFieldName();
            String value = criteria.getValue();
            String value1 = (String) data.get(fieldName);
            return value1 != null && !value1.startsWith(value);
        } else if (operator.equals(OPERATOR_DOES_NOT_END_WITH)) {
            String fieldName = criteria.getFieldName();
            String value = criteria.getValue();
            String value1 = (String) data.get(fieldName);
            return value1 != null && !value1.endsWith(value);
        } else if (operator.equals(OPERATOR_EQUALS)) {
            String fieldName = criteria.getFieldName();
            String value = criteria.getValue();
            String value1 = (String) data.get(fieldName);
            return equalTo(value1, value, false);
        } else if (operator.equals(OPERATOR_NOT_EQUAL)) {
            String fieldName = criteria.getFieldName();
            String value = criteria.getValue();
            String value1 = (String) data.get(fieldName);
            return !equalTo(value1, value, false);
        } else if (operator.equals(OPERATOR_LESS_THAN)) {
            String fieldName = criteria.getFieldName();
            String value = criteria.getValue();
            String value1 = (String) data.get(fieldName);
            return lessThan(value1, value, false);
        } else if (operator.equals(OPERATOR_GREATER_THAN)) {
            String fieldName = criteria.getFieldName();
            String value = criteria.getValue();
            String value1 = (String) data.get(fieldName);
            return greaterThan(value1, value, false);
        } else if (operator.equals(OPERATOR_LESS_THAN_OR_EQUAL_TO)) {
            String fieldName = criteria.getFieldName();
            String value = criteria.getValue();
            String value1 = (String) data.get(fieldName);
            return lessThanOrEqualTo(value1, value, false);
        } else if (operator.equals(OPERATOR_GREATER_THAN_OR_EQUAL_TO)) {
            String fieldName = criteria.getFieldName();
            String value = criteria.getValue();
            String value1 = (String) data.get(fieldName);
            return greaterThanOrEqualTo(value1, value, false);
        } else if (operator.equals(OPERATOR_BETWEEN_INCLUSIVE_MATCH_CASE)) {
            String fieldName = criteria.getFieldName();
            String start = criteria.getStart();
            String end = criteria.getEnd();
            String value1 = (String) data.get(fieldName);
            return greaterThanOrEqualTo(value1, start, false) && lessThanOrEqualTo(value1, end, false);
        } else if (operator.equals(OPERATOR_IS_NULL)) {
            String fieldName = criteria.getFieldName();
            String value1 = (String) data.get(fieldName);
            return value1 == null;
        } else if (operator.equals(OPERATOR_IS_NOT_NULL)) {
            String fieldName = criteria.getFieldName();
            String value1 = (String) data.get(fieldName);
            return value1 != null;
        } else if (operator.equals(OPERATOR_MATCHES_OTHER_FIELD)) {
            String fieldName = criteria.getFieldName();
            String value = (String) data.get(criteria.getValue());
            String value1 = (String) data.get(fieldName);
            return equalTo(value1, value, false);
        } else if (operator.equals(OPERATOR_DIFFERS_FROM_FIELD)) {
            String fieldName = criteria.getFieldName();
            String value = (String) data.get(criteria.getValue());
            String value1 = (String) data.get(fieldName);
            return !equalTo(value1, value, false);
        } else if (operator.equals(OPERATOR_MATCHES_OTHER_FIELD_CASE_INCENSITIVE)) {
            String fieldName = criteria.getFieldName();
            String value = (String) data.get(criteria.getValue());
            String value1 = (String) data.get(fieldName);
            return equalTo(value1, value, true);
        } else if (operator.equals(OPERATOR_DIFFERS_FROM_FIELD_CASE_INCENSITIVE)) {
            String fieldName = criteria.getFieldName();
            String value = (String) data.get(criteria.getValue());
            String value1 = (String) data.get(fieldName);
            return !equalTo(value1, value, true);
        } else if (operator.equals(OPERATOR_LESS_THAN_FIELD)) {
            String fieldName = criteria.getFieldName();
            String value = (String) data.get(criteria.getValue());
            String value1 = (String) data.get(fieldName);
            return lessThan(value1, value, false);
        } else if (operator.equals(OPERATOR_GREATER_THAN_FIELD)) {
            String fieldName = criteria.getFieldName();
            String value = (String) data.get(criteria.getValue());
            String value1 = (String) data.get(fieldName);
            return greaterThan(value1, value, false);
        } else if (operator.equals(OPERATOR_LESS_THAN_OR_EQUAL_TO_FIELD)) {
            String fieldName = criteria.getFieldName();
            String value = (String) data.get(criteria.getValue());
            String value1 = (String) data.get(fieldName);
            return lessThanOrEqualTo(value1, value, false);
        } else if (operator.equals(OPERATOR_GREATER_THAN_OR_EQUAL_TO_FIELD)) {
            String fieldName = criteria.getFieldName();
            String value = (String) data.get(criteria.getValue());
            String value1 = (String) data.get(fieldName);
            return greaterThanOrEqualTo(value1, value, false);
        }
        return false;
    }

    private static boolean isNumber(String v) {
        return NumberUtils.isNumber(v);
    }

    private static boolean lessThan(String value1, String value, boolean ignoreCase) {
        if (value1 == null || value == null) {
            return false;
        }
        if (isNumber(value1) && isNumber(value)) {
            return Double.parseDouble(value1) < Double.parseDouble(value);
        }
        if (ignoreCase) {
            value1 = value1.toLowerCase();
            value = value.toLowerCase();
        }
        return value1.compareTo(value) < 0;
    }

    private static boolean lessThanOrEqualTo(String value1, String value, boolean ignoreCase) {
        if (value1 == null || value == null) {
            return false;
        }
        if (isNumber(value1) && isNumber(value)) {
            return Double.parseDouble(value1) <= Double.parseDouble(value);
        }
        if (ignoreCase) {
            value1 = value1.toLowerCase();
            value = value.toLowerCase();
        }
        return value1.compareTo(value) <= 0;
    }

    private static boolean greaterThan(String value1, String value, boolean ignoreCase) {
        if (value1 == null || value == null) {
            return false;
        }
        if (isNumber(value1) && isNumber(value)) {
            return Double.parseDouble(value1) > Double.parseDouble(value);
        }
        if (ignoreCase) {
            value1 = value1.toLowerCase();
            value = value.toLowerCase();
        }
        return value1.compareTo(value) > 0;
    }

    private static boolean greaterThanOrEqualTo(String value1, String value, boolean ignoreCase) {
        if (value1 == null || value == null) {
            return false;
        }
        if (isNumber(value1) && isNumber(value)) {
            return Double.parseDouble(value1) >= Double.parseDouble(value);
        }
        if (ignoreCase) {
            value1 = value1.toLowerCase();
            value = value.toLowerCase();
        }
        return value1.compareTo(value) >= 0;
    }

    private static boolean equalTo(String value1, String value, boolean ignoreCase) {
        if (value1 == null || value == null) {
            return false;
        }
        if (isNumber(value1) && isNumber(value)) {
            return Double.parseDouble(value1) == Double.parseDouble(value);
        }
        if (ignoreCase) {
            value1 = value1.toLowerCase();
            value = value.toLowerCase();
        }
        return value1.equals(value);
    }

    public static class Criteria {

        private String operator;

        private List<Criteria> subCriteria;

        private String fieldName;

        private String value;

        private String start, end;

        public static Criteria valueOf(String json) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode criteriaNode = mapper.readTree(json);
                if (criteriaNode.has("operator")) {
                    return parseAdvancedCriteriaNode(criteriaNode);
                } else if (criteriaNode.size() > 0) {
                    return parseSimpleCriteriaNode(criteriaNode);
                } else {
                    return null;
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse criteria", e);
            }
        }

        private static Criteria parseSimpleCriteriaNode(JsonNode criteriaNode) {
            Criteria criteria = new Criteria();
            criteria.setOperator(OPERATOR_AND);
            List<Criteria> subCriteria = new ArrayList<Criteria>();
            Iterator<Entry<String, JsonNode>> iter = criteriaNode.getFields();
            while (iter.hasNext()) {
                Entry<String, JsonNode> field = iter.next();
                String fieldName = field.getKey();
                String fieldValue = field.getValue().asText();
                Criteria c = new Criteria();
                c.setOperator(OPERATOR_CONTAINS);
                c.setFieldName(fieldName);
                c.setValue(fieldValue);
                subCriteria.add(c);
            }
            criteria.setSubCriteria(subCriteria);
            return criteria;
        }

        private static Criteria parseAdvancedCriteriaNode(JsonNode criteriaNode) {
            Criteria criteria = new Criteria();
            String operator = criteriaNode.get("operator").isNull()? "and" : criteriaNode.get("operator").asText();
            criteria.setOperator(operator);
            if (operator.equals(OPERATOR_AND) || operator.equals(OPERATOR_OR) || operator.equals(OPERATOR_NOT)) {
                JsonNode subCriteriaNode = criteriaNode.get("criteria");
                List<Criteria> subCriteria = new ArrayList<Criteria>();
                for (int i = 0; i < subCriteriaNode.size(); i++) {
                    subCriteria.add(parseAdvancedCriteriaNode(subCriteriaNode.get(i)));
                }
                criteria.setSubCriteria(subCriteria);
            } else {
                criteria.setFieldName(criteriaNode.get("fieldName").asText());
                if (operator.toLowerCase().contains("between")) {
                    criteria.setStart(criteriaNode.get("start").asText());
                    criteria.setEnd(criteriaNode.get("end").asText());
                } else {
                    criteria.setValue(criteriaNode.get("value").asText());
                }
            }
            return criteria;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }

        public List<Criteria> getSubCriteria() {
            return subCriteria;
        }

        public void setSubCriteria(List<Criteria> subCriteria) {
            this.subCriteria = subCriteria;
        }

        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getStart() {
            return start;
        }

        public void setStart(String start) {
            this.start = start;
        }

        public String getEnd() {
            return end;
        }

        public void setEnd(String end) {
            this.end = end;
        }
    }
}
