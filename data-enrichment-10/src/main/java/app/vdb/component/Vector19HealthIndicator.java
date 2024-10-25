package app.vdb.component;


import app.vdb.service.Vector19Service;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class Vector19HealthIndicator implements HealthIndicator {

    private final Vector19Service vector19Service;

    public Vector19HealthIndicator(Vector19Service vector19Service) {
        this.vector19Service = vector19Service;
    }

    @Override
    public Health health() {
        try {
            vector19Service.ping(); // Check the health
            return Health.up().build();
        } catch (Exception e) {
            return Health.down().withDetail("Error", "Vector19Service is down: " + e.getMessage()).build();
        }
    }
}
