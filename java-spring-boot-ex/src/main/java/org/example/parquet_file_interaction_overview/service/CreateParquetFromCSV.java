package org.example.parquet_file_interaction_overview.service;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class CreateParquetFromCSV {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateParquetFromCSV.class);

    private final SparkSession sparkSession;

    @Autowired
    public CreateParquetFromCSV(SparkSession sparkSession) {
        this.sparkSession = sparkSession;
    }

    public void convertCsvToParquet(String csvFilePath, String parquetOutputDirectory) throws IOException {
        deleteDirectoryIfExists(parquetOutputDirectory);

        Dataset<Row> df = sparkSession.read()
                .option("header", "true")
                .option("inferSchema", "true")
                .csv(csvFilePath);

        df.write()
                .mode("overwrite")
                .parquet(parquetOutputDirectory);

        LOGGER.info("CSV file converted to Parquet successfully! Output: {}", parquetOutputDirectory);
    }

    /**
     * Deletes the given directory and all its contents.
     *
     * On Windows, Java NIO can throw AccessDeniedException when an IDE or file-watcher
     * holds an open handle on the directory. To work around this we delegate to the
     * native {@code rmdir /s /q} command, which succeeds even in that situation.
     *
     * On other platforms a plain recursive NIO walk is used.
     *
     * @param directoryPath path to the directory to delete
     * @throws IOException if deletion fails
     */
    private void deleteDirectoryIfExists(String directoryPath) throws IOException {
        Path path = Paths.get(directoryPath);

        if (!Files.exists(path)) {
            LOGGER.debug("Output directory does not exist yet, skipping delete: {}", directoryPath);
            return;
        }

        LOGGER.info("Deleting existing output directory before writing: {}", directoryPath);

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");

        if (isWindows) {
            deleteWithNativeWindowsCommand(path);
        } else {
            deleteRecursivelyWithNio(path);
        }

        LOGGER.info("Successfully deleted output directory: {}", directoryPath);
    }

    /**
     * Uses {@code cmd /c rmdir /s /q} to recursively delete a directory on Windows.
     * This bypasses Java NIO permission checks that can fail when a process (e.g. an IDE)
     * holds a non-exclusive handle on the directory tree.
     */
    private void deleteWithNativeWindowsCommand(Path path) throws IOException {
        String absolutePath = path.toAbsolutePath().toString();

        ProcessBuilder pb = new ProcessBuilder(
                "cmd", "/c", "rmdir", "/s", "/q", absolutePath
        );
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                String output = new String(process.getInputStream().readAllBytes());
                throw new IOException(
                        "Failed to delete directory '" + absolutePath +
                        "' using rmdir (exit code " + exitCode + "): " + output
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while deleting directory: " + absolutePath, e);
        }
    }

    /**
     * Recursively deletes a directory tree using Java NIO (used on non-Windows platforms).
     */
    private void deleteRecursivelyWithNio(Path root) throws IOException {
        Files.walk(root)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete: " + p, e);
                    }
                });
    }
}
