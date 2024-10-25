package app.vdb.component;


import app.vdb.service.BuyingIntentService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class BuyingIntentHealthIndicator implements HealthIndicator {

    private final BuyingIntentService buyingIntentService;

    public BuyingIntentHealthIndicator(BuyingIntentService buyingIntentService) {
        this.buyingIntentService = buyingIntentService;
    }

    @Override
    public Health health() {
        try {
            buyingIntentService.ping(); // Check the health
            return Health.up().build();
        } catch (Exception e) {
            return Health.down().withDetail("Error", "BuyingIntentService is down: " + e.getMessage()).build();
        }
    }
}

