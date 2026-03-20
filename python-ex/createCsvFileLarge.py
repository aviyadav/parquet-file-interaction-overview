import random

names = ['Alice', 'Bob', 'Charlie', 'David', 'Emma', 'Frank', 'Grace', 'Henry', 'Ivy', 'James', 'Kate', 'Liam', 'Mia', 'Noah', 'Olivia', 'Peter', 'Quinn', 'Rose', 'Sam', 'Tina']
cities = ['New York', 'London', 'Paris', 'Tokyo', 'Sydney', 'Berlin', 'Toronto', 'Mumbai', 'Seoul', 'Dubai', 'Singapore', 'Rome', 'Madrid', 'Amsterdam', 'Vienna', 'Barcelona', 'Munich', 'Stockholm', 'Prague', 'Copenhagen']

with open('my_data-2.csv', 'a') as f:
    for _ in range(100000):
        name = random.choice(names)
        age = random.randint(18, 80)
        city = random.choice(cities)
        f.write(f'{name},{age},{city}\n')