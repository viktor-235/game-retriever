package com.github.viktor235.gameretriever.service;

import com.github.viktor235.gameretriever.exception.AppException;
import liquibase.CatalogAndSchema;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.DiffToChangeLog;
import liquibase.exception.DatabaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.serializer.core.formattedsql.FormattedSqlChangeLogSerializer;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

@Service
@RequiredArgsConstructor
public class LiquibaseService {

    @Value(value = "${spring.datasource.url}")
    private String dbUrl;
    @Value(value = "${spring.jpa.properties.hibernate.default_schema:#{null}}")
    private String dbSchema;
    @Value(value = "${spring.datasource.username:#{null}}")
    private String dbUser;
    @Value(value = "${spring.datasource.password:#{null}}")
    private String dbPassword;
    @Value(value = "${app.changelog.file}")
    @Getter
    private String changelogFile;

    public void generateDataChangelog() {
        generateChangelog(
                getDefaultDb(),
                changelogFile,
                Data.class
        );
    }

    public Database getDefaultDb() {
        return getDb(dbUrl, dbSchema, dbUser, dbPassword);
    }

    public Database getDb(String url, String schema, String user, String password) {
        Properties connectionProps = new Properties();
        if (user != null)
            connectionProps.put("user", dbUser);
        if (password != null)
            connectionProps.put("password", dbPassword);

        try {
            Connection dbConnection = DriverManager.getConnection(url, connectionProps);
            Database db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(dbConnection));
            if (schema != null)
                db.setDefaultSchemaName(schema);
            return db;
        } catch (SQLException | DatabaseException e) {
            throw new AppException("Error while connecting to the DB", e);
        }
    }

    @SafeVarargs
    public final void generateChangelog(Database database, String outputFileName, Class<? extends DatabaseObject>... diffTypes) {
        Liquibase liquibase = new Liquibase(outputFileName, new ClassLoaderResourceAccessor(), database);

        CatalogAndSchema catalogAndSchema = CatalogAndSchema.DEFAULT.standardize(database);

        CompareControl compareControl = new CompareControl();
        DiffToChangeLog changeLogWriter = new DiffToChangeLog(new DiffOutputControl(false, false, false,
                compareControl.getSchemaComparisons())
        );
        changeLogWriter.setChangeSetPath(outputFileName);

        try {
            File file = new File(outputFileName);
            file.getParentFile().mkdir();
            file.createNewFile(); // If file already exists will do nothing
            PrintStream outputStream = new PrintStream(file);
            FormattedSqlChangeLogSerializer changeLogSerializer = new FormattedSqlChangeLogSerializer();
            liquibase.generateChangeLog(catalogAndSchema, changeLogWriter, outputStream, changeLogSerializer, diffTypes);
        } catch (IOException | ParserConfigurationException | DatabaseException e) {
            throw new AppException("Error while generating DB changelog", e);
        }
    }
}
