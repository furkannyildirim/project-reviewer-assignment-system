package com.sakaryateknokent.reviewerassignment.repository;

import com.sakaryateknokent.reviewerassignment.entity.Academician;
import com.sakaryateknokent.reviewerassignment.service.AcademicianTextNormalizer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
@EnabledIfEnvironmentVariable(named = "JWT_SECRET", matches = ".+")
class AcademicianCanonicalIdentityIntegrationTest {

    private final AcademicianRepository academicianRepository;
    private final DataSource dataSource;
    private final List<Long> createdIds = new ArrayList<>();

    @Autowired
    AcademicianCanonicalIdentityIntegrationTest(
            AcademicianRepository academicianRepository,
            DataSource dataSource
    ) {
        this.academicianRepository = academicianRepository;
        this.dataSource = dataSource;
    }

    @AfterEach
    void cleanUp() {
        createdIds.forEach(academicianRepository::deleteById);
        createdIds.clear();
    }

    @Test
    void persistsCanonicalColumnsAndLooksUpEmaillessTurkishIdentity()
            throws SQLException {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        Academician academician = academician(
                "Mahir" + suffix,
                "I\u015F\u0131k",
                "Dr.\u00A0\u00D6\u011Fr.\u00A0\u00DCyesi"
        );

        Academician saved = academicianRepository.saveAndFlush(academician);
        createdIds.add(saved.getId());

        CanonicalColumns columns = canonicalColumns(saved.getId());
        assertEquals(canonical("Mahir" + suffix), columns.firstName());
        assertEquals("\u0131\u015F\u0131k", columns.lastName());
        assertEquals("dr. \u00F6\u011Fr. \u00FCyesi", columns.title());

        List<Academician> matches =
                academicianRepository.findByCanonicalIdentity(
                        columns.firstName(),
                        columns.lastName(),
                        columns.title()
                );

        assertEquals(List.of(saved.getId()), matches.stream()
                .map(Academician::getId)
                .toList());
    }

    @Test
    void repositoryLookupUsesJavaCanonicalForTurkishIForms() {
        String suffix = UUID.randomUUID().toString().replace("-", "");

        assertRoundTrips("I\u015F\u0131k", "\u0131\u015F\u0131k", suffix);
        assertRoundTrips("\u0130pek", "ipek", suffix);
        assertRoundTrips("ISIK", "\u0131s\u0131k", suffix);
        assertRoundTrips("\u0130S\u0130M", "isim", suffix);
    }

    @Test
    void canonicalColumnsPreserveUnicodeWhitespaceBehavior()
            throws SQLException {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        Academician first = save(academician(
                "Whitespace" + suffix + "A",
                "I\u015F\u0131k",
                "Dr. \u00D6\u011Fr. \u00DCyesi"
        ));
        Academician second = save(academician(
                "Whitespace" + suffix + "B",
                "I\u015F\u0131k\u00A0",
                "Dr.\u00A0\u00D6\u011Fr.\u00A0\u00DCyesi"
        ));

        CanonicalColumns firstColumns = canonicalColumns(first.getId());
        CanonicalColumns secondColumns = canonicalColumns(second.getId());

        assertEquals(firstColumns.lastName(), secondColumns.lastName());
        assertEquals(firstColumns.title(), secondColumns.title());
    }

    private void assertRoundTrips(
            String value,
            String expectedCanonical,
            String suffix
    ) {
        Academician saved = save(academician(
                "Canonical" + suffix + value.length(),
                value,
                "Case " + suffix + " " + value.length()
        ));

        List<Academician> matches =
                academicianRepository.findByCanonicalIdentity(
                        canonical(saved.getFirstName()),
                        expectedCanonical,
                        canonical(saved.getTitle())
                );

        assertEquals(List.of(saved.getId()), matches.stream()
                .map(Academician::getId)
                .toList());
    }

    private Academician save(Academician academician) {
        Academician saved = academicianRepository.saveAndFlush(academician);
        createdIds.add(saved.getId());
        return saved;
    }

    private Academician academician(
            String firstName,
            String lastName,
            String title
    ) {
        Academician academician = new Academician();
        academician.setFirstName(firstName);
        academician.setLastName(lastName);
        academician.setTitle(title);
        return academician;
    }

    private CanonicalColumns canonicalColumns(Long id) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT canonical_first_name,
                            canonical_last_name,
                            canonical_title
                     FROM academicians
                     WHERE id = ?
                     """)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return new CanonicalColumns(
                        resultSet.getString("canonical_first_name"),
                        resultSet.getString("canonical_last_name"),
                        resultSet.getString("canonical_title")
                );
            }
        }
    }

    private String canonical(String value) {
        return AcademicianTextNormalizer.canonicalizeIdentityPart(value);
    }

    private record CanonicalColumns(
            String firstName,
            String lastName,
            String title
    ) {
    }
}
