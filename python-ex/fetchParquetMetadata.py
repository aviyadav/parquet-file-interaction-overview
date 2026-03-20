import pyarrow.parquet as pq

parquet_file = pq.ParquetFile('my_data.parquet')
metadata = parquet_file.metadata

print(f"############ File Level Metadata for: {parquet_file} ############")
print(f"Created by: {metadata.created_by}")
print(f"Format version: {metadata.format_version}")
print(f"Number of columns: {metadata.num_columns}")
print(f"Total number of rows: {metadata.num_rows}")
print(f"Number of row groups: {metadata.num_row_groups}")
print(f"Serialized size of metadata (bytes): {metadata.serialized_size}")

for i in range(metadata.num_row_groups):
    row_group_metadata = metadata.row_group(i)
    
    print(f"Row Group {i}:")
    print(f"  Uncompressed Size (total_byte_size): {row_group_metadata.total_byte_size} bytes")
    
    total_compressed_size_rg = 0
    for j in range(row_group_metadata.num_columns):
        column_chunk_metadata = row_group_metadata.column(j)
        compressed_size = column_chunk_metadata.total_compressed_size
        total_compressed_size_rg += compressed_size
        print(f"    Column {j} ({column_chunk_metadata.path_in_schema}) Compressed Size: {compressed_size} bytes, Compression: {column_chunk_metadata.compression}")
    
    print(f"  Total Compressed Size of Row Group {i}: {total_compressed_size_rg} bytes")