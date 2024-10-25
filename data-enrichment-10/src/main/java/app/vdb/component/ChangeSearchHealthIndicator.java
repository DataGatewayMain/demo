package app.vdb.component;

import app.vdb.service.ChangeSearchService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class ChangeSearchHealthIndicator implements HealthIndicator {

    private final ChangeSearchService changeSearchService;

    public ChangeSearchHealthIndicator(ChangeSearchService changeSearchService) {
        this.changeSearchService = changeSearchService;
    }

    @Override
    public Health health() {
        try {
            changeSearchService.ping(); // Check the health
            return Health.up().build();
        } catch (Exception e) {
            return Health.down().withDetail("Error", "ChangeSearchService is down: " + e.getMessage()).build();
        }
    }
}

