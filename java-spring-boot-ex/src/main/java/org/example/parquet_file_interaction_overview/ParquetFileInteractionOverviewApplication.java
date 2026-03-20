package org.example.parquet_file_interaction_overview;

import org.example.parquet_file_interaction_overview.service.CreateParquetFromCSV;
import org.example.parquet_file_interaction_overview.service.ParquetReaderService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;

@SpringBootApplication
public class ParquetFileInteractionOverviewApplication {

    public static void main(String[] args) throws IOException {
        ConfigurableApplicationContext run = SpringApplication.run(ParquetFileInteractionOverviewApplication.class, args);
        ParquetReaderService parquetReaderService = run.getBean(ParquetReaderService.class);
        parquetReaderService.readParquetFile("src/main/resources/inputFiles/my_input.parquet");

        CreateParquetFromCSV createParquetFromCSV = run.getBean(CreateParquetFromCSV.class);
        createParquetFromCSV.convertCsvToParquet("src/main/resources/inputFiles/my_data.csv", "src/main/resources/parquetOutput/");
    }
}
