package app.vdb.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

@Service
public class ExclusionService {

    private static final Logger logger = LoggerFactory.getLogger(ExclusionService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<String> getPidsFromTable(String api) {
        String sql = String.format("SELECT pid FROM %s", api);
        
        try {
            return jdbcTemplate.queryForList(sql, String.class);
        } catch (Exception e) {
            logger.error("Error fetching PIDs from table: {}", api, e);
            return Collections.emptyList();
        }
    }
}
