INSERT INTO crawler_blueprint (code, name, enabled, entry_url, concurrency_limit, config_json, parser_template_code)
VALUES (
    'google-careers-cn',
    'Google Careers — China',
    1,
    'https://www.google.com/about/careers/applications/jobs/results/?location=China',
    1,
    '{"entryUrl": "https://www.google.com/about/careers/applications/jobs/results/?location=China", "paging": {"mode": "QUERY", "parameter": "page", "start": 1, "step": 1}, "rateLimit": {"requestsPerMinute": 10, "burst": 2}, "parser": {"listSelector": "li.lLd3Je", "descriptionField": "description", "fields": {"title": {"type": "TEXT", "selector": "h3.QJPWVe", "required": true}, "company": {"type": "TEXT", "selector": ".RP7SMd span"}, "location": {"type": "TEXT", "selector": ".EAcu5e .pwO9Dc"}, "url": {"type": "ATTRIBUTE", "selector": "a.WpHeLc", "attribute": "href", "baseUrl": "https://careers.google.com"}, "externalId": {"type": "ATTRIBUTE", "selector": "", "attribute": "ssk"}, "description": {"type": "HTML", "selector": ".Xsxa1e"}}}}',
    NULL
);

INSERT INTO job_data_source (code, type, enabled, run_on_startup, require_override, flow, base_options)
VALUES (
    'google-careers-cn',
    'crawler',
    1,
    0,
    0,
    'UNLIMITED',
    '{"blueprintCode":"google-careers-cn","sourceName":"crawler:google-careers","entryUrl":"https://www.google.com/about/careers/applications/jobs/results/?location=China"}'
);

INSERT INTO crawler_blueprint (code, name, enabled, entry_url, concurrency_limit, config_json, parser_template_code)
VALUES (
    'apple-jobs-zh-cn',
    'Apple Jobs - 中国区',
    1,
    'https://jobs.apple.com/zh-cn/search?location=china-CHNC',
    1,
    '{"entryUrl": "https://jobs.apple.com/zh-cn/search?location=china-CHNC", "paging": {"mode": "QUERY", "parameter": "page", "start": 1, "step": 1}, "rateLimit": {"requestsPerMinute": 6, "burst": 1}, "automation": {"enabled": true, "jsEnabled": true, "waitForMilliseconds": 8000}, "flow": [{"type": "WAIT", "options": {"durationMs": 10000}}, {"type": "SCROLL", "options": {"to": "bottom", "times": 3}}, {"type": "WAIT", "options": {"durationMs": 5000}}, {"type": "EXTRACT_LIST"}], "parser": {"baseUrl": "https://jobs.apple.com", "listSelector": "a[href*=\\"/zh-cn/details/\\"]", "fields": {"title": {"type": "TEXT", "selector": ".", "required": true}, "url": {"type": "ATTRIBUTE", "selector": ".", "attribute": "href", "required": true, "baseUrl": "https://jobs.apple.com"}, "externalId": {"type": "ATTRIBUTE", "selector": ".", "attribute": "href", "required": true}, "company": {"type": "CONSTANT", "constant": "Apple", "required": false}, "location": {"type": "CONSTANT", "constant": "中国", "required": false}}, "tagFields": [], "descriptionField": "", "detailFetch": {"enabled": true, "baseUrl": "https://jobs.apple.com", "urlField": "externalId", "delayMs": 2000, "contentSelectors": ["#jd-description", "[data-test=''jd-description'']", ".job-description", ".jd-description", "section[aria-labelledby*=''description'']", "section", "main section", ".main-content section", "div[class*=''job'']", "div[class*=''description'']"]}}}}',
    NULL
);

INSERT INTO job_data_source (code, type, enabled, run_on_startup, require_override, flow, base_options)
VALUES (
    'apple-jobs-crawler',
    'crawler',
    0,
    0,
    0,
    'UNLIMITED',
    '{"blueprintCode":"apple-jobs-zh-cn","entryUrl":"https://jobs.apple.com/zh-cn/search?location=china-CHNC"}'
);

INSERT INTO crawler_blueprint (code, name, enabled, entry_url, concurrency_limit, config_json, parser_template_code)
VALUES (
    'airbnb-china',
    'Airbnb - 中国区',
    1,
    'https://careers.airbnb.com/positions/?_offices=china',
    1,
    '{"entryUrl": "https://careers.airbnb.com/positions/?_offices=china", "paging": {"mode": "NONE"}, "rateLimit": {"requestsPerMinute": 6, "burst": 1}, "automation": {"enabled": true, "jsEnabled": false, "waitForMilliseconds": 5000}, "flow": [{"type": "WAIT", "options": {"durationMs": 5000}}, {"type": "EXTRACT_LIST"}], "parser": {"baseUrl": "https://careers.airbnb.com", "listSelector": "a[href*=\\"/positions/\\"]", "fields": {"title": {"type": "TEXT", "selector": ".", "required": true}, "url": {"type": "ATTRIBUTE", "selector": ".", "attribute": "href", "required": true, "baseUrl": "https://careers.airbnb.com"}, "externalId": {"type": "ATTRIBUTE", "selector": ".", "attribute": "href", "required": true}, "company": {"type": "CONSTANT", "constant": "Airbnb", "required": false}, "location": {"type": "TEXT", "selector": "../../following-sibling::div, ../following-sibling::div, .location", "required": false}}, "tagFields": [], "descriptionField": "", "detailFetch": {"enabled": true, "baseUrl": "https://careers.airbnb.com", "urlField": "url", "delayMs": 3000, "contentSelectors": [".job-description", ".entry-content", "main .content", "article", ".position-description"]}}}}',
    NULL
);

INSERT INTO job_data_source (code, type, enabled, run_on_startup, require_override, flow, base_options)
VALUES (
    'airbnb-crawler',
    'crawler',
    0,
    0,
    0,
    'UNLIMITED',
    '{"blueprintCode":"airbnb-china","entryUrl":"https://careers.airbnb.com/positions/?_offices=china"}'
);
