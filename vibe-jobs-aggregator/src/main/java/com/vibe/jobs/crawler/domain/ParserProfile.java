package com.vibe.jobs.crawler.domain;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.Instant;
import java.util.*;

public class ParserProfile {

    private final String listSelector;
    private final Map<String, ParserField> fields;
    private final Set<String> tagFields;
    private final String descriptionField;
    private final DetailFetchConfig detailFetchConfig;

    private ParserProfile(String listSelector,
                          Map<String, ParserField> fields,
                          Set<String> tagFields,
                          String descriptionField,
                          DetailFetchConfig detailFetchConfig) {
        this.listSelector = listSelector == null ? "" : listSelector.trim();
        this.fields = fields == null ? Map.of() : Map.copyOf(fields);
        this.tagFields = tagFields == null ? Set.of() : Set.copyOf(tagFields);
        this.descriptionField = descriptionField == null ? "" : descriptionField.trim();
        this.detailFetchConfig = detailFetchConfig == null ? DetailFetchConfig.disabled() : detailFetchConfig;
    }

    public static ParserProfile empty() {
        return new ParserProfile("", Map.of(), Set.of(), "", DetailFetchConfig.disabled());
    }

    public static ParserProfile of(String listSelector,
                                   Map<String, ParserField> fields,
                                   Set<String> tagFields,
                                   String descriptionField) {
        return new ParserProfile(listSelector, fields, tagFields, descriptionField, DetailFetchConfig.disabled());
    }

    public static ParserProfile of(String listSelector,
                                   Map<String, ParserField> fields,
                                   Set<String> tagFields,
                                   String descriptionField,
                                   DetailFetchConfig detailFetchConfig) {
        return new ParserProfile(listSelector, fields, tagFields, descriptionField, detailFetchConfig);
    }

    public String listSelector() {
        return listSelector;
    }

    public Map<String, ParserField> fields() {
        return fields;
    }

    public DetailFetchConfig getDetailFetchConfig() {
        return detailFetchConfig;
    }

    public boolean isConfigured() {
        return !listSelector.isBlank() && fields.containsKey("title");
    }

    public List<ParsedJob> parse(String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        Elements elements = org.jsoup.Jsoup.parse(html).select(listSelector);
        if (elements == null || elements.isEmpty()) {
            return List.of();
        }
        List<ParsedJob> result = new ArrayList<>();
        for (Element element : elements) {
            ParsedJob job = parse(element);
            if (job != null) {
                result.add(job);
            }
        }
        return result;
    }

    public ParsedJob parse(Element element) {
        if (element == null) {
            return null;
        }
        Map<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<String, ParserField> entry : fields.entrySet()) {
            Object value = entry.getValue().extract(element);
            if (value == null && entry.getValue().required()) {
                return null;
            }
            if (value != null) {
                values.put(entry.getKey(), value);
            }
        }
        if (!values.containsKey("title")) {
            return null;
        }
        
        String title = Objects.toString(values.get("title"), "").trim();
        // 过滤掉无效的标题
        if (title.isBlank() || 
            title.equals("查看完整的职位描述") || 
            title.equals("提交简历") ||
            title.equals("View full job description") ||
            title.equals("Apply Now") ||
            title.length() < 3) {
            return null;
        }
        
        // 智能生成描述内容
        String description;
        if (descriptionField.isBlank()) {
            // 如果没有指定描述字段，尝试从可用字段构建描述
            StringBuilder desc = new StringBuilder();
            
            // 添加标题
            if (values.containsKey("title")) {
                desc.append("职位: ").append(values.get("title")).append("\n");
            }
            
            // 添加公司
            if (values.containsKey("company")) {
                desc.append("公司: ").append(values.get("company")).append("\n");
            }
            
            // 添加地点
            if (values.containsKey("location")) {
                desc.append("地点: ").append(values.get("location")).append("\n");
            }
            
            // 添加职位级别
            if (values.containsKey("level")) {
                desc.append("级别: ").append(values.get("level")).append("\n");
            }
            
            // 如果构建的描述为空，使用element的文本内容
            if (desc.length() == 0) {
                description = element.text().trim();
                // 如果文本内容也是无用的，使用URL作为描述
                if (description.isBlank() || 
                    description.equals("查看完整的职位描述") || 
                    description.equals("提交简历")) {
                    description = Objects.toString(values.get("url"), "");
                }
            } else {
                description = desc.toString().trim();
            }
        } else {
            description = Objects.toString(values.get(descriptionField), element.html());
        }
        Set<String> tags = new LinkedHashSet<>();
        for (String tagField : tagFields) {
            Object raw = values.get(tagField);
            if (raw instanceof Collection<?> collection) {
                for (Object value : collection) {
                    if (value != null) {
                        String text = value.toString().trim();
                        if (!text.isBlank()) {
                            tags.add(text.toLowerCase(Locale.ROOT));
                        }
                    }
                }
            } else if (raw != null) {
                String text = raw.toString().trim();
                if (!text.isBlank()) {
                    tags.add(text.toLowerCase(Locale.ROOT));
                }
            }
        }
        return new ParsedJob(
                Objects.toString(values.getOrDefault("externalId", UUID.randomUUID().toString()), ""),
                Objects.toString(values.get("title"), ""),
                Objects.toString(values.getOrDefault("company", ""), ""),
                Objects.toString(values.getOrDefault("location", ""), ""),
                Objects.toString(values.getOrDefault("url", ""), ""),
                Objects.toString(values.getOrDefault("level", ""), ""),
                values.get("postedAt") instanceof Instant instant ? instant : null,
                tags,
                description
        );
    }

    public record ParsedJob(String externalId,
                             String title,
                             String company,
                             String location,
                             String url,
                             String level,
                             Instant postedAt,
                             Set<String> tags,
                             String description) {
    }

    /**
     * 详情获取配置
     */
    public static class DetailFetchConfig {
        private final boolean enabled;
        private final String baseUrl;
        private final String urlField;  // 用于构建详情URL的字段名，如"url"或"externalId"
        private final List<String> contentSelectors;  // 用于提取详情内容的CSS选择器列表
        private final long delayMs;  // 请求间延迟毫秒数
        
        private DetailFetchConfig(boolean enabled, String baseUrl, String urlField, 
                                 List<String> contentSelectors, long delayMs) {
            this.enabled = enabled;
            this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
            this.urlField = urlField == null ? "url" : urlField.trim();
            this.contentSelectors = contentSelectors == null ? List.of() : List.copyOf(contentSelectors);
            this.delayMs = Math.max(0, delayMs);
        }
        
        public static DetailFetchConfig disabled() {
            return new DetailFetchConfig(false, "", "url", List.of(), 0);
        }
        
        public static DetailFetchConfig of(String baseUrl, String urlField, List<String> contentSelectors) {
            return new DetailFetchConfig(true, baseUrl, urlField, contentSelectors, 1000);
        }
        
        public static DetailFetchConfig of(String baseUrl, String urlField, List<String> contentSelectors, long delayMs) {
            return new DetailFetchConfig(true, baseUrl, urlField, contentSelectors, delayMs);
        }
        
        public boolean isEnabled() { return enabled; }
        public String getBaseUrl() { return baseUrl; }
        public String getUrlField() { return urlField; }
        public List<String> getContentSelectors() { return contentSelectors; }
        public long getDelayMs() { return delayMs; }
        public Boolean shouldPreserveHtml() { return null; } // 临时添加，后续完善
    }
}
