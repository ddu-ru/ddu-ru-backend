package com.dduru.gildongmu.migration;

import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationVersionUniquenessTest {
    @Test
    void versionedSqlMigrationsHaveUniqueFlywayVersions() throws Exception {
        var resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:db/migration/V*.sql");
        assertThat(resources).as("versioned migrations must be present").isNotEmpty();
        var namePattern = Pattern.compile("V(.+)__(.+)\\.sql");
        var filesByVersion = new TreeMap<MigrationVersion, ArrayList<String>>();
        for (var resource : resources) {
            var filename = resource.getFilename();
            var matcher = namePattern.matcher(filename);
            assertThat(matcher.matches()).as("migration name: %s", filename).isTrue();
            var version = MigrationVersion.fromVersion(matcher.group(1));
            filesByVersion.computeIfAbsent(version, ignored -> new ArrayList<>()).add(filename);
        }
        var duplicates = filesByVersion.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(Map.Entry::toString).toList();
        assertThat(duplicates).as("duplicate Flyway versions").isEmpty();
    }
}
