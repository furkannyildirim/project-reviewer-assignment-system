package db.migration;

import com.sakaryateknokent.reviewerassignment.service.AcademicianTextNormalizer;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class V11__add_canonical_identity_to_academicians
        extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    ALTER TABLE academicians
                        ADD COLUMN canonical_first_name VARCHAR(100)
                    """);
            statement.execute("""
                    ALTER TABLE academicians
                        ADD COLUMN canonical_last_name VARCHAR(100)
                    """);
            statement.execute("""
                    ALTER TABLE academicians
                        ADD COLUMN canonical_title VARCHAR(100)
                    """);
        }

        try (PreparedStatement select = context.getConnection()
                .prepareStatement("""
                        SELECT id, first_name, last_name, title
                        FROM academicians
                        ORDER BY id
                        """);
             PreparedStatement update = context.getConnection()
                     .prepareStatement("""
                             UPDATE academicians
                             SET canonical_first_name = ?,
                                 canonical_last_name = ?,
                                 canonical_title = ?
                             WHERE id = ?
                             """)) {
            try (ResultSet resultSet = select.executeQuery()) {
                while (resultSet.next()) {
                    update.setString(1, canonical(resultSet.getString(
                            "first_name"
                    )));
                    update.setString(2, canonical(resultSet.getString(
                            "last_name"
                    )));
                    update.setString(3, canonical(resultSet.getString(
                            "title"
                    )));
                    update.setLong(4, resultSet.getLong("id"));
                    update.addBatch();
                }
            }

            update.executeBatch();
        }

        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    ALTER TABLE academicians
                        ALTER COLUMN canonical_first_name SET NOT NULL,
                        ALTER COLUMN canonical_last_name SET NOT NULL,
                        ALTER COLUMN canonical_title SET NOT NULL
                    """);
            statement.execute("""
                    CREATE INDEX idx_academicians_canonical_identity
                        ON academicians (
                            canonical_first_name,
                            canonical_last_name,
                            canonical_title
                        )
                    """);
        }
    }

    private String canonical(String value) {
        return AcademicianTextNormalizer.canonicalizeIdentityPart(value);
    }
}
