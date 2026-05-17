import random

genres = ["Fiction", "Non-Fiction", "Mystery", "Fantasy", "Science Fiction", "Biography", "Romance", "Thriller", "History", "Self-Help"]
authors = ["Paulo Coelho", "James Clear", "J.K. Rowling", "Stephen King", "Agatha Christie", "George R.R. Martin", "Dan Brown", "Ernest Hemingway", "Mark Twain", "F. Scott Fitzgerald"]
images = [
    "https://res.cloudinary.com/demo/image/upload/sample.jpg",
    "https://res.cloudinary.com/demo/image/upload/v1/samples/landscapes/beach-boat.jpg",
    "https://res.cloudinary.com/demo/image/upload/v1/samples/food/fish-vegetables.jpg",
    "https://res.cloudinary.com/demo/image/upload/v1/samples/people/kitchen-bar.jpg",
    "https://res.cloudinary.com/demo/image/upload/v1/samples/animals/reindeer.jpg"
]

sql = "USE booknest_book;\nDELETE FROM books;\nINSERT INTO books (book_id, title, author, description, price, stock, genre, featured, cover_image_url) VALUES \n"

rows = []
for i in range(1, 201):
    genre = random.choice(genres)
    author = random.choice(authors)
    img = random.choice(images)
    title = f"{genre} Masterpiece Vol {i}"
    price = round(random.uniform(299, 1299), 2)
    stock = random.randint(5, 50)
    featured = 1 if random.random() < 0.15 else 0
    desc = f"An incredible {genre} journey written by the world-renowned {author}."
    
    rows.append(f"({i}, '{title}', '{author}', '{desc}', {price}, {stock}, '{genre}', {featured}, '{img}')")

sql += ",\n".join(rows) + ";"

with open("seed_200_books.sql", "w") as f:
    f.write(sql)

print("✅ Created seed_200_books.sql with 200 entries!")
