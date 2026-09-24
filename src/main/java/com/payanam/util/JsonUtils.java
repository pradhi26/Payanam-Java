package com.payanam.util;

import java.util.*;

public class JsonUtils {

    public static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) {
            return "\"" + escapeString((String) obj) + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append(toJson(String.valueOf(entry.getKey())));
                sb.append(":");
                sb.append(toJson(entry.getValue()));
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof Collection) {
            Collection<?> col = (Collection<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : col) {
                if (!first) sb.append(",");
                first = false;
                sb.append(toJson(item));
            }
            sb.append("]");
            return sb.toString();
        }
        if (obj instanceof com.payanam.model.TourPackage) {
            com.payanam.model.TourPackage p = (com.payanam.model.TourPackage) obj;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("destination", p.getDestination());
            m.put("duration", p.getDuration());
            m.put("price", p.getPrice());
            m.put("rating", p.getRating());
            m.put("category", p.getCategory());
            m.put("description", p.getDescription());
            m.put("imageUrl", p.getImageUrl());
            m.put("inclusions", p.getInclusions());
            return toJson(m);
        }
        if (obj instanceof com.payanam.model.Booking) {
            com.payanam.model.Booking b = (com.payanam.model.Booking) obj;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.getId());
            m.put("customer", b.getCustomer());
            m.put("phone", b.getPhone());
            m.put("userEmail", b.getUserEmail());
            m.put("destination", b.getDestination());
            m.put("packageId", b.getPackageId());
            m.put("travelDate", b.getTravelDate());
            m.put("travellers", b.getTravellers());
            m.put("amount", b.getAmount());
            m.put("status", b.getStatus());
            m.put("bookedAt", b.getBookedAt());
            return toJson(m);
        }
        if (obj instanceof com.payanam.model.Review) {
            com.payanam.model.Review r = (com.payanam.model.Review) obj;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("user", r.getUser());
            m.put("destination", r.getDestination());
            m.put("rating", r.getRating());
            m.put("comment", r.getComment());
            m.put("createdAt", r.getCreatedAt());
            return toJson(m);
        }
        if (obj instanceof com.payanam.model.User) {
            com.payanam.model.User u = (com.payanam.model.User) obj;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            m.put("email", u.getEmail());
            m.put("role", u.getRole());
            m.put("createdAt", u.getCreatedAt());
            return toJson(m);
        }
        return "\"" + escapeString(obj.toString()) + "\"";
    }

    public static String escapeString(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 32) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    public static Map<String, Object> parseObject(String json) {
        if (json == null) return new HashMap<>();
        String trimmed = json.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        } else {
            return new HashMap<>();
        }
        
        Map<String, Object> map = new LinkedHashMap<>();
        int i = 0;
        int len = trimmed.length();
        
        while (i < len) {
            // Find key
            while (i < len && Character.isWhitespace(trimmed.charAt(i))) i++;
            if (i >= len) break;
            
            if (trimmed.charAt(i) != '"') break;
            int keyStart = ++i;
            while (i < len && (trimmed.charAt(i) != '"' || trimmed.charAt(i - 1) == '\\')) i++;
            String key = unescapeString(trimmed.substring(keyStart, i));
            i++; // skip closing quote
            
            // Skip colon
            while (i < len && (Character.isWhitespace(trimmed.charAt(i)) || trimmed.charAt(i) == ':')) i++;
            if (i >= len) break;
            
            // Parse value
            int valueStart = i;
            if (trimmed.charAt(i) == '"') {
                int strStart = ++i;
                while (i < len && (trimmed.charAt(i) != '"' || trimmed.charAt(i - 1) == '\\')) i++;
                String val = unescapeString(trimmed.substring(strStart, i));
                map.put(key, val);
                i++;
            } else if (trimmed.charAt(i) == '{') {
                int braceCount = 1;
                i++;
                while (i < len && braceCount > 0) {
                    if (trimmed.charAt(i) == '{') braceCount++;
                    else if (trimmed.charAt(i) == '}') braceCount--;
                    i++;
                }
                map.put(key, parseObject(trimmed.substring(valueStart, i)));
            } else if (trimmed.charAt(i) == '[') {
                int bracketCount = 1;
                i++;
                while (i < len && bracketCount > 0) {
                    if (trimmed.charAt(i) == '[') bracketCount++;
                    else if (trimmed.charAt(i) == ']') bracketCount--;
                    i++;
                }
                map.put(key, parseArray(trimmed.substring(valueStart, i)));
            } else {
                while (i < len && trimmed.charAt(i) != ',' && trimmed.charAt(i) != '}') i++;
                String rawVal = trimmed.substring(valueStart, i).trim();
                if ("true".equalsIgnoreCase(rawVal)) {
                    map.put(key, Boolean.TRUE);
                } else if ("false".equalsIgnoreCase(rawVal)) {
                    map.put(key, Boolean.FALSE);
                } else if ("null".equalsIgnoreCase(rawVal)) {
                    map.put(key, null);
                } else {
                    try {
                        if (rawVal.contains(".")) {
                            map.put(key, Double.parseDouble(rawVal));
                        } else {
                            map.put(key, Long.parseLong(rawVal));
                        }
                    } catch (NumberFormatException e) {
                        map.put(key, rawVal);
                    }
                }
            }
            
            // Skip comma
            while (i < len && (Character.isWhitespace(trimmed.charAt(i)) || trimmed.charAt(i) == ',')) i++;
        }
        
        return map;
    }

    public static List<Object> parseArray(String json) {
        List<Object> list = new ArrayList<>();
        if (json == null) return list;
        String trimmed = json.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        } else {
            return list;
        }
        
        int i = 0;
        int len = trimmed.length();
        while (i < len) {
            while (i < len && Character.isWhitespace(trimmed.charAt(i))) i++;
            if (i >= len) break;
            
            if (trimmed.charAt(i) == '{') {
                int start = i;
                int braceCount = 1;
                i++;
                while (i < len && braceCount > 0) {
                    if (trimmed.charAt(i) == '{') braceCount++;
                    else if (trimmed.charAt(i) == '}') braceCount--;
                    i++;
                }
                list.add(parseObject(trimmed.substring(start, i)));
            } else if (trimmed.charAt(i) == '"') {
                int strStart = ++i;
                while (i < len && (trimmed.charAt(i) != '"' || trimmed.charAt(i - 1) == '\\')) i++;
                list.add(unescapeString(trimmed.substring(strStart, i)));
                i++;
            } else {
                int start = i;
                while (i < len && trimmed.charAt(i) != ',') i++;
                String rawVal = trimmed.substring(start, i).trim();
                if (!rawVal.isEmpty()) {
                    if ("true".equalsIgnoreCase(rawVal)) list.add(Boolean.TRUE);
                    else if ("false".equalsIgnoreCase(rawVal)) list.add(Boolean.FALSE);
                    else if ("null".equalsIgnoreCase(rawVal)) list.add(null);
                    else {
                        try {
                            if (rawVal.contains(".")) list.add(Double.parseDouble(rawVal));
                            else list.add(Long.parseLong(rawVal));
                        } catch (Exception e) {
                            list.add(rawVal);
                        }
                    }
                }
            }
            while (i < len && (Character.isWhitespace(trimmed.charAt(i)) || trimmed.charAt(i) == ',')) i++;
        }
        return list;
    }

    private static String unescapeString(String s) {
        if (s == null) return "";
        return s.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }
}
