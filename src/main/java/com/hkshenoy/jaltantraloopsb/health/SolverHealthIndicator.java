package com.hkshenoy.jaltantraloopsb.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
// import org.springframework.context.annotation.Bean;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
public class SolverHealthIndicator implements HealthIndicator {

    private static final String SOLVER_PATH = "/home/sayantan/jaltantra_dev/Jaltantra_branch_loop_v_2.3/JalTantra-Code-and-Scripts";

    @Override
    public Health health() {
        System.out.println(">>> SolverHealthIndicator invoked");

        boolean exists = Files.exists(Paths.get(SOLVER_PATH));
        return exists
                ? Health.up().withDetail("solver", "Available").build()
                : Health.down().withDetail("solver", "Missing").build();
    }
}
