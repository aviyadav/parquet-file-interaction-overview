import csv

# data to be written to a CSV file
data = [
    ['Name', 'Age', 'City'],  
    ['Alice', 30, 'New York'],
    ['Bob', 24, 'London'],
    ['Charlie', 35, 'Paris']
]

# file path
csv_file_path = 'my_data.csv'

with open(csv_file_path, 'w', newline='') as file:
    writer = csv.writer(file)

    # Write the header row
    writer.writerow(data[0])

    # Write the remaining data rows
    writer.writerows(data[1:])

print(f'CSV file "{csv_file_path}" created successfully.')