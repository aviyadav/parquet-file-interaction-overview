package org.example.parquet_file_interaction_overview.config;

import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Configuration
public class SparkConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(SparkConfig.class);

    @Bean
    public SparkSession sparkSession() throws IOException {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            configureHadoopHomeForWindows();
        }

        return SparkSession.builder()
                .appName("CsvToParquetConverter")
                .master("local[*]")
                .config("spark.ui.enabled", "false")
                .config("spark.driver.host", "localhost")
                .getOrCreate();
    }

    private void configureHadoopHomeForWindows() throws IOException {
        // First, try the project-local hadoop/ directory (contains real winutils.exe)
        File projectHadoopHome = new File(System.getProperty("user.dir"), "hadoop");
        File winutilsExe = new File(projectHadoopHome, "bin/winutils.exe");

        if (winutilsExe.exists()) {
            System.setProperty("hadoop.home.dir", projectHadoopHome.getAbsolutePath());
            LOGGER.info("Using project-local Hadoop home with winutils.exe: {}", projectHadoopHome.getAbsolutePath());
        } else {
            // Fallback: create a temporary hadoop home directory with an empty bin/ folder.
            // Spark will still log a winutils warning but will not crash in local mode.
            File tempHadoopHome = Files.createTempDirectory("hadoop-home").toFile();
            new File(tempHadoopHome, "bin").mkdirs();
            System.setProperty("hadoop.home.dir", tempHadoopHome.getAbsolutePath());
            LOGGER.warn(
                "winutils.exe not found at {}. Falling back to temporary Hadoop home: {}. "
                    + "Some file-system operations may fail. "
                    + "Place winutils.exe in <project-root>/hadoop/bin/ to suppress this.",
                winutilsExe.getAbsolutePath(),
                tempHadoopHome.getAbsolutePath()
            );
        }
    }
}
