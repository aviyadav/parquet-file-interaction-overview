package org.example.parquet_file_interaction_overview.service;

import org.apache.avro.generic.GenericData;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ParquetReaderService {

    Logger LOGGER = LoggerFactory.getLogger(ParquetReaderService.class);

    public void readParquetFile(String parquetFilePath) throws IOException {
        Path path = new Path(parquetFilePath);
        Configuration conf = new Configuration();

        try (ParquetReader<GenericData.Record> reader = AvroParquetReader
                .<GenericData.Record>builder(HadoopInputFile.fromPath(path, conf))
                .build()) {
            GenericData.Record record;
            LOGGER.info("Parquet file: {} content", parquetFilePath);
            while ((record = reader.read()) != null) {
                // data processing here...
                LOGGER.info(String.valueOf(record));
            }
        }
    }
}