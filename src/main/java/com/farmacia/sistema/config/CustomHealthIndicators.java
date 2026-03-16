package com.farmacia.sistema.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;

@Component("database")
class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(3)) {
                return Health.up()
                        .withDetail("database", conn.getMetaData().getDatabaseProductName())
                        .withDetail("url", conn.getMetaData().getURL())
                        .build();
            }
            return Health.down().withDetail("reason", "Connection not valid").build();
        } catch (Exception e) {
            return Health.down().withException(e).build();
        }
    }
}

@Component("diskSpace")
class DiskSpaceHealthIndicator implements HealthIndicator {

    private static final long THRESHOLD_BYTES = 100 * 1024 * 1024; // 100 MB

    @Override
    public Health health() {
        File disk = new File(".");
        long free = disk.getFreeSpace();
        long total = disk.getTotalSpace();
        long usedPercent = total > 0 ? ((total - free) * 100) / total : 0;

        Health.Builder builder = free >= THRESHOLD_BYTES ? Health.up() : Health.down();
        return builder
                .withDetail("free_mb", free / (1024 * 1024))
                .withDetail("total_mb", total / (1024 * 1024))
                .withDetail("used_percent", usedPercent + "%")
                .build();
    }
}

@Component("memoria")
class MemoryHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        Runtime rt = Runtime.getRuntime();
        long maxMb = rt.maxMemory() / (1024 * 1024);
        long totalMb = rt.totalMemory() / (1024 * 1024);
        long freeMb = rt.freeMemory() / (1024 * 1024);
        long usedMb = totalMb - freeMb;
        long usedPercent = maxMb > 0 ? (usedMb * 100) / maxMb : 0;

        Health.Builder builder = usedPercent < 90 ? Health.up() : Health.down();
        return builder
                .withDetail("max_mb", maxMb)
                .withDetail("used_mb", usedMb)
                .withDetail("free_mb", freeMb)
                .withDetail("used_percent", usedPercent + "%")
                .build();
    }
}
