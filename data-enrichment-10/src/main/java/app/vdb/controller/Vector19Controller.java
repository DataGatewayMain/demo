package app.vdb.controller;

import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import app.vdb.dto.SearchRequest;
import app.vdb.entity.Vector19;
import app.vdb.service.BuyingIntentService;
import app.vdb.service.ChangeSearchService;
import app.vdb.service.ExclusionService;
import app.vdb.service.Vector19Service;
import jakarta.persistence.criteria.Predicate;
import org.springframework.jdbc.core.JdbcTemplate;


import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@CrossOrigin(origins = {"http://localhost:4200", "http://192.168.0.5:4200", "https://test.vectordb.app/", "https://vectordb.app/"})
public class Vector19Controller {

    private static final Logger logger = LoggerFactory.getLogger(Vector19Controller.class);

    @Autowired
    private Vector19Service vector19Service;
    
    @Autowired
    private ExclusionService exclusionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private BuyingIntentService buyingIntentService;
    
    @Autowired
    private ChangeSearchService changeSearchService;
    
    @GetMapping("/v1/health/check")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> healthStatus = new HashMap<>();

        try {
            buyingIntentService.ping();
            healthStatus.put("BuyingIntentService", "UP");
        } catch (Exception e) {
            healthStatus.put("BuyingIntentService", "DOWN: " + e.getMessage());
        }

        try {
            changeSearchService.ping();
            healthStatus.put("ChangeSearchService", "UP");
        } catch (Exception e) {
            healthStatus.put("ChangeSearchService", "DOWN: " + e.getMessage());
        }

        try {
            vector19Service.ping();
            healthStatus.put("Vector19Service", "UP");
        } catch (Exception e) {
            healthStatus.put("Vector19Service", "DOWN: " + e.getMessage());
        }

        return ResponseEntity.ok(healthStatus);
    }
    
    
    
    @PostMapping("/v1/search/buying-intent")
    public ResponseEntity<Map<String, Object>> getBuyingIntent() {
        List<Map<String, Object>> commonCompanyNames = buyingIntentService.getCommonCompanyNames();

        if (commonCompanyNames == null) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "No valid API keys found"));
        }

        // Build the response
        return ResponseEntity.ok(Map.of("common_company_names", commonCompanyNames, "threshold", commonCompanyNames.size()));
    }

    @PostMapping("/v1/search/changes")
    public ResponseEntity<Map<String, Object>> searchForChanges(@RequestBody Map<String, Object> request) {
        // Extract the API from the request body
        String api = (String) request.get("api");

        if (api == null || api.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "API value is required"));
        }

        // Convert the API to table name (assuming the table name is the same as API)
        String tableName = api;

        // Call the service to find changes without pagination
        List<Map<String, Object>> updatedResults = changeSearchService.findChanges(tableName);

        // Get the total count of changes
        int totalRecords = updatedResults.size();

        // Build the response
        Map<String, Object> response = new HashMap<>();
        response.put("updated_data", updatedResults);
        response.put("updated_count", totalRecords);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/v1/search/net-new")
    public ResponseEntity<Map<String, Object>> search(@RequestBody SearchRequest request) {
        int zeroBasedPage = request.getPage() > 0 ? request.getPage() - 1 : 0;

        Map<String, String> filters = request.getFilters();
        if (filters == null) {
            filters = new HashMap<>();
        }

        Map<String, String> validParams = filterValidParams(filters);

        if (validParams.isEmpty()) {
            long totalCount = vector19Service.countWithExclusions(request.getApi());
            Map<String, Object> response = new HashMap<>();
            response.put("net_new_count", totalCount);
            return ResponseEntity.ok(response);
        }

        Specification<Vector19> spec = buildSpecification(validParams, request.getApi(), true);
        Pageable pageable = PageRequest.of(zeroBasedPage, request.getSize());

        Page<Vector19> resultPage = vector19Service.search(spec, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("net_new_data", resultPage.getContent());
        response.put("net_new_pagination", generatePaginationInfo(resultPage));
        response.put("net_new_count", resultPage.getTotalElements());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/v1/search/saved")
    public ResponseEntity<Map<String, Object>> searchApi(@RequestBody SearchRequest request) {
        int zeroBasedPage = request.getPage() > 0 ? request.getPage() - 1 : 0;

        Map<String, String> filters = request.getFilters();
        if (filters == null) {
            filters = new HashMap<>();
        }

        Map<String, String> validParams = filterValidParams(filters);

        Specification<Vector19> spec = buildSpecification(validParams, null, false);
        Pageable pageable = PageRequest.of(zeroBasedPage, request.getSize());

        Page<Vector19> resultPage = vector19Service.searchWithIncludedPids(spec, pageable, request.getApi());

        Map<String, Object> response = new HashMap<>();
        response.put("saved_data", resultPage.getContent());
        response.put("saved_pagination", generatePaginationInfo1(resultPage));
        response.put("saved_count", resultPage.getTotalElements());

        return ResponseEntity.ok(response);
    }

   
    @PostMapping("/v1/search/total")
    public ResponseEntity<Map<String, Object>> searchVector19(@RequestBody SearchRequest request) {
        int zeroBasedPage = request.getPage() > 0 ? request.getPage() - 1 : 0;

        Map<String, String> filters = request.getFilters();
        if (filters == null) {
            filters = new HashMap<>();
        }

        Map<String, String> validParams = filterValidParams(filters);

        if (validParams.isEmpty()) {
            long totalCount = vector19Service.countWithExclusions(null);
            Map<String, Object> response = new HashMap<>();
            response.put("total_count", totalCount);
            return ResponseEntity.ok(response);
        }

        Specification<Vector19> spec = buildSpecification(validParams, null, false);
        Pageable pageable = PageRequest.of(zeroBasedPage, request.getSize());

        Page<Vector19> resultPage = vector19Service.search(spec, pageable);

        List<String> userProvidedPids = exclusionService.getPidsFromTable(request.getApi());

        List<Vector19> updatedResults = resultPage.getContent().stream().map(vector19 -> {
            boolean isSaved = userProvidedPids.contains(vector19.getPid());
            return vector19;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("total_data", updatedResults);
        response.put("total_count", resultPage.getTotalElements());
        response.put("total_pages", generatePaginationInfo2(resultPage));

        return ResponseEntity.ok(response);
    }

    private static final int BATCH_SIZE = 100000;
    private List<List<String>> splitIntoBatches(List<String> values, int batchSize) {
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < values.size(); i += batchSize) {
            int end = Math.min(values.size(), i + batchSize);
            batches.add(values.subList(i, end));
        }
        return batches;
    }

    private Map<String, String> filterValidParams(Map<String, String> allParams) {
        Set<String> entityFields = Arrays.stream(Vector19.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        Map<String, String> validParams = new HashMap<>();
        allParams.forEach((key, value) -> {
            if ((key.startsWith("include_") || key.startsWith("exclude_")) && !value.isEmpty() &&
                    entityFields.contains(key.replace("include_", "").replace("exclude_", ""))) {
                validParams.put(key, value.replace(", ", ","));
            }
        });
        return validParams;
    }

    private Specification<Vector19> buildSpecification(Map<String, String> validParams, String api, boolean useExclusion) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            validParams.forEach((key, value) -> {
                if (key.startsWith("include_")) {
                    String field = key.replace("include_", "");
                    List<String> values = Arrays.asList(value.split(","));
                    
                    // Split values into batches
                    List<List<String>> batches = splitIntoBatches(values, BATCH_SIZE);
                    for (List<String> batch : batches) {
                        predicates.add(root.get(field).in(batch));
                    }
                } else if (key.startsWith("exclude_")) {
                    String field = key.replace("exclude_", "");
                    List<String> values = Arrays.asList(value.split(","));
                    
                    // Split values into batches for exclusion as well
                    List<List<String>> batches = splitIntoBatches(values, BATCH_SIZE);
                    for (List<String> batch : batches) {
                        predicates.add(criteriaBuilder.not(root.get(field).in(batch)));
                    }
                }
            });

            if (useExclusion && api != null && !api.isEmpty()) {
                List<String> excludePids = exclusionService.getPidsFromTable(api);
                if (!excludePids.isEmpty()) {
                    List<List<String>> batches = splitIntoBatches(excludePids, BATCH_SIZE);
                    for (List<String> batch : batches) {
                        predicates.add(criteriaBuilder.not(root.get("pid").in(batch)));
                    }
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }


    private Map<String, Object> generatePaginationInfo(Page<Vector19> resultPage) {
        Map<String, Object> paginationInfo = new HashMap<>();
        paginationInfo.put("current_page_net_new", resultPage.getNumber() + 1);
        paginationInfo.put("records_per_page", resultPage.getSize());
        paginationInfo.put("total_pages_net_new", resultPage.getTotalPages());
        return paginationInfo;
    }
    
    private Map<String, Object> generatePaginationInfo1(Page<Vector19> resultPage) {
        Map<String, Object> paginationInfo = new HashMap<>();
        paginationInfo.put("current_page_saved", resultPage.getNumber() + 1);
        paginationInfo.put("records_per_page", resultPage.getSize());
        paginationInfo.put("total_pages_saved", resultPage.getTotalPages());
        return paginationInfo;
    }
    
    private Map<String, Object> generatePaginationInfo2(Page<Vector19> resultPage) {
        Map<String, Object> paginationInfo = new HashMap<>();
        paginationInfo.put("current_page_total", resultPage.getNumber() + 1);
        paginationInfo.put("records_per_page_total", resultPage.getSize());
        paginationInfo.put("total_pages_total", resultPage.getTotalPages());
        return paginationInfo;
    }
}
