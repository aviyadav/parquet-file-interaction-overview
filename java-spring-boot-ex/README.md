# Parquet File Interaction Overview

A Spring Boot application demonstrating how to read existing Parquet files and convert CSV data into Parquet format using Apache Spark and Apache Parquet/Avro on the local filesystem.

---

## Table of Contents

- [Project Overview](#project-overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [How to Run](#how-to-run)
- [What the Application Does](#what-the-application-does)
- [Issues Found & Fixes Applied](#issues-found--fixes-applied)

---

## Project Overview

This project was created to explore interaction with the [Apache Parquet](https://parquet.apache.org/) columnar file format from a Spring Boot application. It showcases two core operations:

1. **Reading a Parquet file** — using Apache Parquet's Avro reader (`parquet-avro`) directly with Hadoop's `Configuration` and `Path` APIs.
2. **Converting CSV to Parquet** — using Apache Spark's `SparkSession` to read a CSV file and write it out as a Parquet file with Snappy compression.

Both operations are wired as Spring-managed `@Service` beans and invoked from the application's `main` method on startup.

---

## Tech Stack

| Dependency | Version |
|---|---|
| Java (compiler target) | 17 |
| Spring Boot | 3.5.7 |
| Apache Spark (`spark-sql_2.12`) | 3.5.7 |
| Apache Hadoop (`hadoop-client`) | 3.4.3 |
| Apache Parquet Avro (`parquet-avro`) | 1.17.0 |
| Embedded server | Tomcat (port `8083`) |

---

## Project Structure

```
parquet-file-interaction-overview/
├── hadoop/
│   └── bin/
│       ├── hadoop.dll          # Windows native library for Hadoop NativeIO
│       └── winutils.exe        # Windows utility required by Hadoop/Spark
├── src/
│   └── main/
│       ├── java/org/example/parquet_file_interaction_overview/
│       │   ├── ParquetFileInteractionOverviewApplication.java   # Entry point
│       │   ├── config/
│       │   │   └── SparkConfig.java          # SparkSession Spring @Bean
│       │   └── service/
│       │       ├── ParquetReaderService.java  # Reads a Parquet file via Avro
│       │       └── CreateParquetFromCSV.java  # Converts CSV → Parquet via Spark
│       └── resources/
│           ├── application.properties
│           └── inputFiles/
│               ├── my_data.csv           # Input CSV file
│               └── my_input.parquet      # Input Parquet file to be read
└── pom.xml
```

> **Note:** The `parquetOutput/` directory under `src/main/resources/` is created at runtime by Spark and will contain the generated `.snappy.parquet` file and a `_SUCCESS` marker.

---

## Prerequisites

| Requirement | Notes |
|---|---|
| **JDK 17 or higher** | Compiled targeting Java 17; tested on Java 25. |
| **Apache Maven 3.6+** | Used to build and run the project. |
| **Windows OS** (if applicable) | The `hadoop/bin/winutils.exe` and `hadoop.dll` binaries are already bundled in the repository for Windows support. No separate Hadoop installation is required. |

No full Hadoop or Spark installation is needed — all dependencies are pulled in via Maven and Spark runs in **local mode** (`local[*]`).

---

## How to Run

Clone the repository and run the following command from the project root:

```bash
mvn spring-boot:run
```

Maven will automatically pass the required JVM flags (see [`pom.xml`](pom.xml)) and the application will:

1. Start the embedded Tomcat server on port `8083`.
2. Initialise a local Spark session.
3. Read and log the records from `src/main/resources/inputFiles/my_input.parquet`.
4. Convert `src/main/resources/inputFiles/my_data.csv` to Parquet and write the output to `src/main/resources/parquetOutput/`.

Expected log lines on a successful run:

```
INFO  o.e.p.config.SparkConfig          : Using project-local Hadoop home with winutils.exe: <project-root>\hadoop
INFO  arquetFileInteractionOverviewApplication : Started ParquetFileInteractionOverviewApplication in X seconds
INFO  o.e.p.service.ParquetReaderService : Parquet file: src/main/resources/inputFiles/my_input.parquet content
INFO  o.e.p.service.CreateParquetFromCSV : CSV file converted to Parquet successfully! Output: src/main/resources/parquetOutput/
```

---

## What the Application Does

### `ParquetReaderService`
Uses `AvroParquetReader` together with Hadoop's `HadoopInputFile` to open an existing `.parquet` file and iterate over every `GenericData.Record`, logging each row to the console. This approach works without Spark and is suitable for lightweight read-only scenarios.

### `CreateParquetFromCSV`
Uses a Spring-injected `SparkSession` (running in local mode) to:
- Read a CSV file with a header row and inferred schema.
- Write the resulting `Dataset<Row>` as Parquet with `overwrite` mode.

Before Spark attempts to write, the service pre-deletes the output directory using the native OS command (`rmdir /s /q` on Windows, recursive NIO walk on other platforms) to prevent Spark's own Hadoop-based directory cleanup from failing on Windows.

### `SparkConfig`
A Spring `@Configuration` class that produces the `SparkSession` bean. On Windows it also programmatically sets the `hadoop.home.dir` system property to the bundled `hadoop/` directory so that `winutils.exe` is discoverable. If the binary is missing it falls back to a temporary directory and logs a warning.

---

## Issues Found & Fixes Applied

The application failed to start with multiple cascading errors when first run. All issues were Windows / Java-version specific. Below is each issue, its root cause, and the exact fix that was applied.

---

### Issue 1 — Java Module System: `sun.nio.ch.DirectBuffer` Inaccessible

**Symptom**

```
Error creating bean with name 'sparkSession': Failed to instantiate [org.apache.spark.sql.SparkSession]:
Factory method 'sparkSession' threw exception with message:
class org.apache.spark.storage.StorageUtils$ (in unnamed module) cannot access class
sun.nio.ch.DirectBuffer (in module java.base) because module java.base does not export
sun.nio.ch to unnamed module
```

**Root Cause**

The project targets Java 17 at compile time but was executed on **Java 25**. Spark 3.5.x internally accesses several sealed JDK packages (`sun.nio.ch`, `sun.nio.cs`, `sun.security.action`, etc.) that are hidden behind the Java Platform Module System (JPMS) introduced in Java 9. Without explicit `--add-opens` flags, the JVM blocks this reflective access and the `SparkSession` bean cannot be created, causing the entire application context to fail.

**Fix — `pom.xml`**

Added a `<jvmArguments>` block to the `spring-boot-maven-plugin` configuration, opening all internal packages that Spark requires:

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <jvmArguments>
            -Djava.library.path=${project.basedir}/hadoop/bin
            --add-opens=java.base/java.lang=ALL-UNNAMED
            --add-opens=java.base/java.lang.invoke=ALL-UNNAMED
            --add-opens=java.base/java.lang.reflect=ALL-UNNAMED
            --add-opens=java.base/java.io=ALL-UNNAMED
            --add-opens=java.base/java.net=ALL-UNNAMED
            --add-opens=java.base/java.nio=ALL-UNNAMED
            --add-opens=java.base/java.util=ALL-UNNAMED
            --add-opens=java.base/java.util.concurrent=ALL-UNNAMED
            --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED
            --add-opens=java.base/sun.nio.ch=ALL-UNNAMED
            --add-opens=java.base/sun.nio.cs=ALL-UNNAMED
            --add-opens=java.base/sun.security.action=ALL-UNNAMED
            --add-opens=java.base/sun.util.calendar=ALL-UNNAMED
            --add-opens=java.security.jgss/sun.security.krb5=ALL-UNNAMED
        </jvmArguments>
    </configuration>
</plugin>
```

---

### Issue 2 — `HADOOP_HOME` Unset on Windows

**Symptom**

```
WARN  org.apache.hadoop.util.Shell : Did not find winutils.exe: {}
java.io.FileNotFoundException: HADOOP_HOME and hadoop.home.dir are unset.
    -see https://cwiki.apache.org/confluence/display/HADOOP2/WindowsProblems
```

**Root Cause**

Apache Hadoop (used internally by Spark) requires the `HADOOP_HOME` environment variable or the `hadoop.home.dir` system property to be set on Windows so it can locate `winutils.exe` — a small utility that emulates POSIX file-permission calls that Hadoop relies on. Neither was configured in the development environment, so Hadoop's `Shell` class threw a `FileNotFoundException` during static initialisation, which cascaded into a `SparkSession` creation failure.

**Fix — `SparkConfig.java` + bundled binaries**

`winutils.exe` and `hadoop.dll` (built for Hadoop 3.3.5, binary-compatible with 3.4.x) were downloaded from [cdarlint/winutils](https://github.com/cdarlint/winutils) and committed to the repository under `hadoop/bin/`.

`SparkConfig` was updated to set `hadoop.home.dir` programmatically before building the `SparkSession`:

```java
private void configureHadoopHomeForWindows() throws IOException {
    File projectHadoopHome = new File(System.getProperty("user.dir"), "hadoop");
    File winutilsExe = new File(projectHadoopHome, "bin/winutils.exe");

    if (winutilsExe.exists()) {
        System.setProperty("hadoop.home.dir", projectHadoopHome.getAbsolutePath());
        LOGGER.info("Using project-local Hadoop home with winutils.exe: {}", projectHadoopHome.getAbsolutePath());
    } else {
        // Graceful fallback — Spark will warn but won't crash in local mode
        File tempHadoopHome = Files.createTempDirectory("hadoop-home").toFile();
        new File(tempHadoopHome, "bin").mkdirs();
        System.setProperty("hadoop.home.dir", tempHadoopHome.getAbsolutePath());
        LOGGER.warn("winutils.exe not found at {}. Falling back to temporary Hadoop home: {}.",
            winutilsExe.getAbsolutePath(), tempHadoopHome.getAbsolutePath());
    }
}
```

Two additional Spark configs were also added to the session builder to improve Windows local-mode stability:

```java
.config("spark.ui.enabled", "false")   // disables the Spark Web UI (avoids port-binding issues)
.config("spark.driver.host", "localhost")
```

---

### Issue 3 — `UnsatisfiedLinkError`: `NativeIO$Windows.access0` Not Linked

**Symptom**

```
java.lang.UnsatisfiedLinkError:
'boolean org.apache.hadoop.io.nativeio.NativeIO$Windows.access0(java.lang.String, int)'
```

**Root Cause**

Hadoop's `NativeIO$Windows` class uses JNI to delegate Windows ACL permission checks (`access0`) to `hadoop.dll`. Even though `hadoop.dll` was present in `hadoop/bin/`, the JVM's native library search path (`java.library.path`) did not include that directory, so the dynamic linker could not resolve the JNI symbol at runtime.

**Fix — `pom.xml`**

Added `-Djava.library.path=${project.basedir}/hadoop/bin` to the `<jvmArguments>` in `pom.xml` (shown in full in Issue 1's fix above). This tells the JVM to look in `hadoop/bin/` when loading native libraries, allowing `hadoop.dll` to be found and `access0` to be resolved correctly.

---

### Issue 4 — Output Directory Cannot Be Deleted Before Spark Write

**Symptom (first form — Spark's own Hadoop delete)**

```
org.apache.spark.SparkException: Unable to clear output directory
file:/C:/.../src/main/resources/parquetOutput prior to writing to it.
```

**Symptom (second form — Java NIO fallback)**

```
java.nio.file.AccessDeniedException: src\main\resources\parquetOutput
```

**Root Cause**

When Spark writes in `overwrite` mode it first tries to delete the existing output directory via Hadoop's `FileSystem` API (which requires `winutils.exe` to work correctly on Windows). Even after `winutils.exe` was in place, the second attempt using Java NIO's `Files.walkFileTree` raised `AccessDeniedException` — a common Windows behaviour when an IDE (e.g. IntelliJ IDEA) or the OS file indexer holds an open handle on the directory, preventing its deletion through standard Java I/O.

**Fix — `CreateParquetFromCSV.java`**

The service now pre-deletes the output directory itself, before handing control to Spark, using the native Windows `rmdir /s /q` command (which succeeds even when a process holds a non-exclusive handle). On non-Windows platforms a plain recursive NIO walk is used instead:

```java
private void deleteDirectoryIfExists(String directoryPath) throws IOException {
    Path path = Paths.get(directoryPath);
    if (!Files.exists(path)) return;

    if (System.getProperty("os.name").toLowerCase().contains("windows")) {
        deleteWithNativeWindowsCommand(path);   // cmd /c rmdir /s /q
    } else {
        deleteRecursivelyWithNio(path);         // Files.walk + reverseOrder delete
    }
}
```

Because the directory is removed before Spark's write begins, Spark's own `overwrite` cleanup step finds no existing directory and proceeds without error.

---

### Summary Table

| # | Error | Root Cause | Fix Location |
|---|---|---|---|
| 1 | `sun.nio.ch.DirectBuffer` inaccessible | JPMS seals internal JDK packages; Spark needs them | `pom.xml` — `--add-opens` JVM flags |
| 2 | `HADOOP_HOME` and `hadoop.home.dir` unset | No Hadoop installation; `winutils.exe` missing | `SparkConfig.java` + `hadoop/bin/winutils.exe` committed to repo |
| 3 | `UnsatisfiedLinkError: access0` | `hadoop.dll` not on `java.library.path` | `pom.xml` — `-Djava.library.path` JVM flag |
| 4 | Output directory cannot be deleted | IDE/OS file handle blocks NIO & Hadoop delete on Windows | `CreateParquetFromCSV.java` — native `rmdir` pre-delete |