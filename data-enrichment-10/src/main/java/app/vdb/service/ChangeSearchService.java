package app.vdb.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ChangeSearchService {

    private static final Logger logger = LoggerFactory.getLogger(ChangeSearchService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void ping() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        } catch (Exception e) {
            logger.error("Database ping failed", e);
        }
    }

    public List<Map<String, Object>> findChanges(String tableName) {
        String sql = "SELECT v19.pid, " +
                     "v19.email_address AS v19_email, " +
                     "v19.company_name AS v19_company, " +
                     "v19.company_domain AS v19_domain, " +
                     "v19.job_title AS v19_title, " +
                     "api.email_address AS email_address, " +
                     "api.company_name AS company_name, " +
                     "api.company_domain AS company_domain, " +
                     "api.job_title AS job_title " +
                     "FROM vector19 v19 " +
                     "JOIN " + tableName + " api ON v19.pid = api.pid " +
                     "WHERE v19.email_address != api.email_address OR " +
                     "v19.company_name != api.company_name OR " +
                     "v19.company_domain != api.company_domain OR " +
                     "v19.job_title != api.job_title";

        try {
            return jdbcTemplate.queryForList(sql);
        } catch (BadSqlGrammarException e) {
            // Handle the specific exception but don't log it to the console
            return Collections.emptyList();
        } catch (Exception e) {
            // Log other exceptions if necessary
            logger.error("Error querying for changes from table: {}", tableName, e);
            return Collections.emptyList();
        }
    }


    public int countChanges(String tableName) {
        String sql = "SELECT COUNT(*) " +
                     "FROM vector19 v19 " +
                     "JOIN " + tableName + " api ON v19.pid = api.pid " +
                     "WHERE v19.email_address != api.email_address OR " +
                     "v19.company_name != api.company_name OR " +
                     "v19.company_domain != api.company_domain OR " +
                     "v19.job_title != api.job_title";

        try {
            return jdbcTemplate.queryForObject(sql, Integer.class);
        } catch (Exception e) {
            logger.error("Error counting changes in table: {}", tableName, e);
            return 0;
        }
    }
}
