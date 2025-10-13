package com.vibe.jobs.crawler.domain;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParserField {

    private static final Logger log = LoggerFactory.getLogger(ParserField.class);

    private final String name;
    private final ParserFieldType type;
    private final String selector;
    private final String attribute;
    private final String constant;
    private final String format;
    private final String delimiter;
    private final boolean required;
    private final String baseUrl;

    public ParserField(String name,
                       ParserFieldType type,
                       String selector,
                       String attribute,
                       String constant,
                       String format,
                       String delimiter,
                       boolean required,
                       String baseUrl) {
        this.name = name == null ? "" : name.trim();
        this.type = type == null ? ParserFieldType.TEXT : type;
        this.selector = selector == null ? "" : selector.trim();
        this.attribute = attribute == null ? "" : attribute.trim();
        this.constant = constant == null ? "" : constant.trim();
        this.format = format == null ? "" : format.trim();
        this.delimiter = delimiter == null ? "," : delimiter;
        this.required = required;
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
    }

    // 保持向后兼容的构造器
    public ParserField(String name,
                       ParserFieldType type,
                       String selector,
                       String attribute,
                       String constant,
                       String format,
                       String delimiter,
                       boolean required) {
        this(name, type, selector, attribute, constant, format, delimiter, required, null);
    }

    public static ParserField of(String name, ParserFieldType type, String selector) {
        return new ParserField(name, type, selector, null, null, null, ",", false, null);
    }

    public String name() {
        return name;
    }

    public ParserFieldType type() {
        return type;
    }

    public String selector() {
        return selector;
    }

    public String attribute() {
        return attribute;
    }

    public boolean required() {
        return required;
    }

    public Object extract(Element element) {
        if (element == null) {
            return null;
        }
        return switch (type) {
            case CONSTANT -> constant;
            case HTML -> select(element).stream().findFirst().map(Element::html).orElse(null);
            case ATTRIBUTE -> {
                String rawValue = select(element).stream().findFirst()
                        .map(el -> attribute.isBlank() ? el.text() : el.attr(attribute))
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .orElse(null);

                if (rawValue != null && "href".equals(attribute)) {
                    if (!rawValue.startsWith("http://") && !rawValue.startsWith("https://")) {
                        String resolvedBaseUrl = !baseUrl.isBlank() ? baseUrl : extractBaseUrl(element);
                        if (resolvedBaseUrl != null) {
                            String separator = resolvedBaseUrl.endsWith("/") || rawValue.startsWith("/") ? "" : "/";
                            yield resolvedBaseUrl + separator + (rawValue.startsWith("/") ? rawValue.substring(1) : rawValue);
                        }
                    }
                }
                yield rawValue;
            }
            case LIST -> parseList(element);
            case DATE -> parseDate(element);
            case TEXT -> {
                // 首先尝试使用指定的选择器
                String textValue = select(element).stream().findFirst()
                        .map(Element::text)
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .orElse(null);
                
                // 如果是location字段且没有提取到值，或者值无效，使用智能location提取
                if ("location".equalsIgnoreCase(name)) {
                    if (textValue == null || !isValidLocationText(textValue)) {
                        String intelligentLocation = extractLocationIntelligently(element);
                        if (intelligentLocation != null) {
                            log.info("Location field enhanced: '{}' -> '{}'", textValue, intelligentLocation);
                            textValue = intelligentLocation;
                        }
                    }
                }
                
                yield textValue;
            }
        };
    }

    private List<Element> select(Element element) {
        if (selector == null || selector.isBlank() || selector.trim().isEmpty()) {
            return List.of();
        }

        // Clean the selector to avoid empty string issues
        String cleanSelector = selector.trim();
        if (".".equals(cleanSelector)) {
            return Collections.singletonList(element);
        }
        if (cleanSelector.isEmpty()) {
            return Collections.singletonList(element);
        }
        
        try {
            Elements elements = element.select(cleanSelector);
            if (elements == null || elements.isEmpty()) {
                return List.of();
            }
            return elements.stream().toList();
        } catch (Exception e) {
            log.info("Selector parsing failed for '{}': {}", cleanSelector, e.getMessage());
            if (cleanSelector.contains(",")) {
                List<Element> aggregated = new java.util.ArrayList<>();
                for (String part : cleanSelector.split(",")) {
                    String trimmed = part == null ? "" : part.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    
                    // Skip XPath-style selectors that JSoup can't handle, but try conversion first
                    if (isXPathSelector(trimmed)) {
                        String converted = convertXPathToCss(trimmed);
                        if (!converted.equals(trimmed) && !converted.trim().isEmpty()) {
                            log.info("Converting XPath selector '{}' to CSS '{}'", trimmed, converted);
                            try {
                                Elements partial = element.select(converted);
                                if (partial != null && !partial.isEmpty()) {
                                    aggregated.addAll(partial.stream().toList());
                                }
                                continue;
                            } catch (Exception conversionException) {
                                log.info("CSS conversion '{}' failed, skipping", converted);
                            }
                        }
                        // 只记录一次XPath警告，避免日志噪音
                        if (log.isTraceEnabled()) {
                            log.trace("Skipping XPath selector '{}' (JSoup supports CSS only)", trimmed);
                        }
                        continue;
                    }
                    
                    if (".".equals(trimmed)) {
                        aggregated.add(element);
                        continue;
                    }
                    try {
                        Elements partial = element.select(trimmed);
                        if (partial != null && !partial.isEmpty()) {
                            aggregated.addAll(partial.stream().toList());
                        }
                    } catch (Exception ignored) {
                        log.info("Sub-selector parsing failed for '{}': {}", trimmed, ignored.getMessage());
                    }
                }
                if (!aggregated.isEmpty()) {
                    return aggregated;
                }
            }
            // As a fallback return the current element when selector is invalid
            return Collections.singletonList(element);
        }
    }
    
    /**
     * 检测是否为XPath样式的选择器（JSoup不支持）
     */
    private boolean isXPathSelector(String selector) {
        return selector.contains("../") || 
               selector.contains("../../") || 
               selector.contains("following-sibling::") ||
               selector.contains("preceding-sibling::") ||
               selector.contains("parent::") ||
               selector.contains("child::");
    }
    
    /**
     * 尝试将XPath样式的选择器转换为等效的CSS选择器
     */
    private String convertXPathToCss(String xpathSelector) {
        // 简单的XPath到CSS转换
        String converted = xpathSelector.trim();
        
        // 处理一些常见的XPath模式
        if (converted.contains("following-sibling::div")) {
            if (converted.contains("../following-sibling::div")) {
                // 父级的兄弟元素：在CSS中无法直接表达，尝试使用通用兄弟选择器
                converted = converted.replace("../following-sibling::div", "~ div");
            }
            if (converted.contains("../../following-sibling::div")) {
                // 祖父级兄弟元素：在CSS中无法直接表达，尝试使用通用兄弟选择器
                converted = converted.replace("../../following-sibling::div", "~ div");
            }
        }
        
        // 清理可能残留的XPath语法
        converted = converted.replace("../", "");
        converted = converted.replace("../../", "");
        
        return converted;
    }

    private Object parseDate(Element element) {
        List<Element> elements = select(element);
        if (elements.isEmpty()) {
            return null;
        }
        String value = elements.get(0).text();
        value = value == null ? null : value.trim();
        if (value == null || value.isBlank()) {
            return null;
        }
        if (format.isBlank()) {
            try {
                return Instant.parse(value);
            } catch (Exception ignored) {
                return null;
            }
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format, Locale.ROOT).withZone(ZoneOffset.UTC);
            return formatter.parse(value, Instant::from);
        } catch (Exception e) {
            return null;
        }
    }

    private Object parseList(Element element) {
        List<Element> elements = select(element);
        if (elements.isEmpty()) {
            return List.of();
        }
        String value = elements.get(0).text();
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String[] parts = value.split(delimiter == null || delimiter.isEmpty() ? "," : delimiter);
        return Arrays.stream(parts)
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .toList();
    }

    /**
     * 智能location提取 - 通用能力，适用于各种网站结构
     */
    private String extractLocationIntelligently(Element element) {
        // 策略1: 从当前元素及其父元素的文本中提取location
        String locationFromText = extractLocationFromText(element);
        if (locationFromText != null) {
            log.info("Location extracted from element text: '{}'", locationFromText);
            return locationFromText;
        }
        
        // 策略2: 使用选择器搜索
        String[] locationSelectors = {
            // 直接的location类和属性
            ".location", "[data-location]", "[class*='location']", 
            ".job-location", ".position-location", ".work-location",
            
            // 通用的地理位置相关类
            ".office", ".workplace", ".city", ".country", ".region",
            "[class*='office']", "[class*='city']", "[class*='place']",
            
            // 现代前端框架常用的data属性
            "[data-testid*='location']", "[data-test*='location']",
            "[data-cy*='location']", "[aria-label*='location']",
            
            // 兄弟元素和相邻元素（适用于标题旁边的位置信息）
            "~ span", "~ div", "+ span", "+ div", "~ *", "+ *"
        };
        
        for (String sel : locationSelectors) {
            try {
                Elements found = element.select(sel);
                for (Element elem : found) {
                    String text = elem.text().trim();
                    if (isValidLocationText(text)) {
                        log.info("Location extracted using selector '{}': '{}'", sel, text);
                        return text;
                    }
                }
            } catch (Exception e) {
                // 忽略选择器错误，继续尝试下一个
                log.trace("Selector '{}' failed: {}", sel, e.getMessage());
            }
        }
        
        // 策略3: 在父元素或祖父元素中寻找location信息
        Element parent = element.parent();
        if (parent != null) {
            String locationFromParent = extractLocationFromElementTree(parent, 2);
            if (locationFromParent != null) {
                log.info("Location extracted from parent element: '{}'", locationFromParent);
                return locationFromParent;
            }
        }
        
        // 策略4: URL参数推断（如?_offices=china）
        String locationFromUrl = extractLocationFromUrl(element);
        if (locationFromUrl != null) {
            log.info("Location extracted from URL context: '{}'", locationFromUrl);
            return locationFromUrl;
        }
        
        log.info("No location information found for element: {}", 
                 element.text().length() > 50 ? element.text().substring(0, 50) + "..." : element.text());
        return null;
    }
    
    /**
     * 从元素文本中提取location信息（处理如"Job Title, China"这样的格式）
     */
    private String extractLocationFromText(Element element) {
        // 检查当前元素及其父元素的文本
        Element current = element;
        for (int i = 0; i < 3 && current != null; i++) {
            String text = current.text().trim();
            if (!text.isEmpty()) {
                String location = parseLocationFromText(text);
                if (location != null) {
                    return location;
                }
            }
            current = current.parent();
        }
        return null;
    }
    
    /**
     * 从文本中解析location信息
     */
    private String parseLocationFromText(String text) {
        if (text == null || text.length() < 3) {
            return null;
        }
        
        // 模式1: "Title, Location" 格式
        if (text.contains(",")) {
            String[] parts = text.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (isValidLocationText(trimmed)) {
                    return trimmed;
                }
            }
        }
        
        // 模式2: 直接包含location关键词
        String[] locationKeywords = {
            "China", "Beijing", "Shanghai", "Shenzhen", "Guangzhou", "Hangzhou",
            "中国", "北京", "上海", "深圳", "广州", "杭州",
            "Singapore", "Hong Kong", "Taiwan", "Macau"
        };
        
        for (String keyword : locationKeywords) {
            if (text.contains(keyword)) {
                // 尝试提取包含关键词的词组
                String[] words = text.split("\\s+");
                for (int i = 0; i < words.length; i++) {
                    if (words[i].contains(keyword)) {
                        // 返回关键词及其前后词组
                        StringBuilder location = new StringBuilder();
                        int start = Math.max(0, i - 1);
                        int end = Math.min(words.length, i + 2);
                        for (int j = start; j < end; j++) {
                            if (location.length() > 0) location.append(" ");
                            location.append(words[j].replaceAll("[^\\w\\s,-]", ""));
                        }
                        String result = location.toString().trim();
                        if (isValidLocationText(result)) {
                            return result;
                        }
                        // 如果词组无效，至少返回关键词本身
                        return keyword;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * 从URL上下文推断location（如?_offices=china）
     */
    private String extractLocationFromUrl(Element element) {
        try {
            String docUrl = element.ownerDocument() != null ? element.ownerDocument().location() : null;
            if (docUrl != null && docUrl.contains("_offices=china")) {
                return "China";
            }
            if (docUrl != null && docUrl.contains("location=china")) {
                return "China";
            }
        } catch (Exception e) {
            log.trace("Failed to extract location from URL: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 从元素树中提取location信息
     */
    private String extractLocationFromElementTree(Element element, int maxDepth) {
        if (element == null || maxDepth <= 0) {
            return null;
        }
        
        // 在当前元素的所有子元素中寻找location信息
        Elements allDescendants = element.select("*");
        for (Element desc : allDescendants) {
            String text = desc.text().trim();
            if (isValidLocationText(text) && !text.equals(element.text().trim())) {
                return text;
            }
        }
        
        // 递归检查兄弟元素
        Element nextSibling = element.nextElementSibling();
        if (nextSibling != null) {
            String siblingLocation = extractLocationFromElementTree(nextSibling, maxDepth - 1);
            if (siblingLocation != null) {
                return siblingLocation;
            }
        }
        
        return null;
    }
    
    /**
     * 验证文本是否为有效的location信息
     */
    private boolean isValidLocationText(String text) {
        if (text == null || text.isBlank() || text.length() < 2) {
            return false;
        }
        
        // 排除明显不是location的文本
        String lower = text.toLowerCase();
        if (lower.matches("^[0-9]+$") || // 纯数字
            lower.matches("^[a-z]$") ||   // 单个字母
            lower.contains("apply") || lower.contains("view") || lower.contains("submit") ||
            lower.contains("description") || lower.contains("detail") ||
            lower.length() > 100) { // 太长的文本通常不是location
            return false;
        }
        
        // 常见location关键词
        String[] locationKeywords = {
            "china", "beijing", "shanghai", "shenzhen", "guangzhou", "hangzhou",
            "中国", "北京", "上海", "深圳", "广州", "杭州",
            "singapore", "hong kong", "taiwan", "macau",
            "remote", "hybrid", "office", "onsite"
        };
        
        for (String keyword : locationKeywords) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        
        // 检查是否包含城市/国家的常见模式
        if (text.matches(".*[A-Z][a-z]+,\\s*[A-Z][a-z]+.*") || // "City, Country"格式
            text.matches(".*\\p{IsHan}+.*")) { // 包含中文字符
            return true;
        }
        
        return false;
    }
    
    private String extractBaseUrl(Element element) {
        // 尝试从document的base标签获取
        if (element.ownerDocument() != null) {
            org.jsoup.nodes.Element base = element.ownerDocument().selectFirst("base[href]");
            if (base != null) {
                String baseHref = base.attr("href");
                if (!baseHref.isBlank()) {
                    return baseHref.endsWith("/") ? baseHref.substring(0, baseHref.length() - 1) : baseHref;
                }
            }
        }
        
        // 尝试从当前页面URL推断（如果有的话）
        String docLocation = element.ownerDocument() != null ? element.ownerDocument().location() : null;
        if (docLocation != null && !docLocation.isBlank()) {
            try {
                java.net.URL url = new java.net.URL(docLocation);
                return url.getProtocol() + "://" + url.getHost() + (url.getPort() != -1 && url.getPort() != 80 && url.getPort() != 443 ? ":" + url.getPort() : "");
            } catch (Exception ignored) {
                // Fallback to hardcoded for Apple Jobs (this is a compromise)
                if (docLocation.contains("jobs.apple.com")) {
                    return "https://jobs.apple.com";
                }
            }
        }
        
        // 最后的fallback - 如果能从element的HTML中推断出来
        String outerHtml = element.outerHtml();
        if (outerHtml.contains("jobs.apple.com")) {
            return "https://jobs.apple.com";
        }
        
        return null;
    }
}
