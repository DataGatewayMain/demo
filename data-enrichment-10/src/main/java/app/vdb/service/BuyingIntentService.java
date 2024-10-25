package app.vdb.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class BuyingIntentService {

	 @Autowired
	    private JdbcTemplate jdbcTemplate;
	    
	    public void ping() {
	        // Execute a simple query to check the database connection
	        jdbcTemplate.queryForObject("SELECT 1", Integer.class);
	    }
	    

    public List<Map<String, Object>> getCommonCompanyNames() {
        // Step 1: Fetch api_keys from the user table
        String apiKeysQuery = "SELECT api_key FROM user";
        List<String> apiKeys = jdbcTemplate.queryForList(apiKeysQuery, String.class);

        // Filter out null or empty api_keys
        List<String> tableNames = apiKeys.stream()
                                         .filter(Objects::nonNull)
                                         .filter(apiKey -> !apiKey.isEmpty())
                                         .collect(Collectors.toList());

        if (tableNames.isEmpty()) {
            return null; // Return null if no valid API keys found
        }

        // Step 2: Create query to find common company_name that appears in at least 75% of tables
        StringBuilder sqlBuilder = new StringBuilder();
        int tableCount = tableNames.size();
        int minAppearances = (int) Math.ceil(tableCount * 0.75);  // 75% threshold

        sqlBuilder.append("SELECT company_name, COUNT(DISTINCT table_name) as table_count FROM (");

        for (int i = 0; i < tableNames.size(); i++) {
            if (i > 0) {
                sqlBuilder.append(" UNION ALL ");
            }
            sqlBuilder.append("SELECT company_name, '").append(tableNames.get(i)).append("' as table_name FROM ").append(tableNames.get(i));
        }

        sqlBuilder.append(") combined ")
                  .append("GROUP BY company_name ")
                  .append("HAVING COUNT(DISTINCT table_name) >= ").append(minAppearances)
                  .append(" AND company_name IN (");

        // Subquery to count occurrences of each company_name in all tables
        for (int i = 0; i < tableNames.size(); i++) {
            if (i > 0) {
                sqlBuilder.append(" UNION ALL ");
            }
            sqlBuilder.append("SELECT company_name FROM ").append(tableNames.get(i))
                      .append(" GROUP BY company_name HAVING COUNT(*) > 2");
        }

        sqlBuilder.append(")");

        String sql = sqlBuilder.toString();

        // Step 3: Execute the query and fetch results
        List<Map<String, Object>> commonCompanyNames;
        try {
            commonCompanyNames = jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            throw new RuntimeException("SQL execution error: " + e.getMessage(), e);
        }

        return commonCompanyNames;
    }
}
