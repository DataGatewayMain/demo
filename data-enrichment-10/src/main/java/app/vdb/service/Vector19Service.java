package app.vdb.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.vdb.entity.Vector19;
import app.vdb.repository.Vector19Repository;

import java.util.List;
import java.util.Collections;

@Service
public class Vector19Service {

    private static final Logger logger = LoggerFactory.getLogger(Vector19Service.class);

    @Autowired
    private Vector19Repository vector19Repository;

    @Autowired
    private ExclusionService exclusionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void ping() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        } catch (Exception e) {
            logger.error("Database ping failed", e);
        }
    }

    public Page<Vector19> search(Specification<Vector19> spec, Pageable pageable) {
        try {
            return vector19Repository.findAll(spec, pageable);
        } catch (Exception e) {
            logger.error("Error executing search with specification", e);
            return Page.empty(pageable);
        }
    }

    public Page<Vector19> searchWithIncludedPids(Specification<Vector19> spec, Pageable pageable, String api) {
        try {
            List<String> includePids = exclusionService.getPidsFromTable(api);
            if (includePids.isEmpty()) {
                return Page.empty(pageable);
            }
            spec = spec.and((root, query, criteriaBuilder) -> root.get("pid").in(includePids));
            return vector19Repository.findAll(spec, pageable);
        } catch (Exception e) {
            logger.error("Error executing search with included PIDs for table: {}", api, e);
            return Page.empty(pageable);
        }
    }

    public long countWithExclusions(String api) {
        try {
            if (api != null && !api.isEmpty()) {
                List<String> excludePids = exclusionService.getPidsFromTable(api);
                if (!excludePids.isEmpty()) {
                    return vector19Repository.count((root, query, criteriaBuilder) ->
                            criteriaBuilder.not(root.get("pid").in(excludePids))
                    );
                }
            }
            return vector19Repository.count();
        } catch (Exception e) {
            logger.error("Error counting with exclusions for table: {}", api, e);
            return 0;
        }
    }
}
