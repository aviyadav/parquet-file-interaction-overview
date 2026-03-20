import pandas as pd
import pyarrow as pa
import pyarrow.parquet as pq

# specify input and output file paths
csv_file_path = 'my_data.csv'
parquet_file_path = 'my_data.parquet'

# read input file
df = pd.read_csv(csv_file_path)

# convert the read dataframe to PyArrow Table
table = pa.Table.from_pandas(df)

# write the table to a Parquet file
pq.write_table(table, parquet_file_path)

print(f"CSV '{csv_file_path}' successfully converted to Parquet '{parquet_file_path}'")