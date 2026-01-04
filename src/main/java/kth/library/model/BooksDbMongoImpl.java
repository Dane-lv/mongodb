package kth.library.model;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;

import kth.library.model.exceptions.ConnectionException;
import kth.library.model.exceptions.InsertException;
import kth.library.model.exceptions.SelectException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;

/**
 * MongoDB implementation of the IBooksDb interface.
 * Handles connections and CRUD operations against a MongoDB database.
 * 
 * @author Student Name
 */
public class BooksDbMongoImpl implements IBooksDb {

    private MongoClient mongoClient;
    private MongoDatabase database;
    
    private static final String MONGO_USER = "library_user";
    private static final String MONGO_PASS = "lib123";
    private static final String MONGO_HOST = "localhost";
    private static final int MONGO_PORT = 27017;
    private static final String DB_NAME = "library_db";

    private static final String BOOKS_COLLECTION = "books";
    private static final String AUTHORS_COLLECTION = "authors";
    private static final String GENRES_COLLECTION = "genres";
    private static final String USERS_COLLECTION = "users";
    private static final String COUNTERS_COLLECTION = "counters";

    public BooksDbMongoImpl() {
    }

    /**
     * Connects to the MongoDB database using the specified credentials.
     * 
     * @param databaseUrl Not used in this implementation (credentials are internal).
     * @return true if connection is successful.
     * @throws ConnectionException if connection fails.
     */
    @Override
    public boolean connect(String databaseUrl) throws ConnectionException {
        String connectionString = String.format("mongodb://%s:%s@%s:%d/%s", 
                MONGO_USER, MONGO_PASS, MONGO_HOST, MONGO_PORT, DB_NAME);
        
        try {
            mongoClient = MongoClients.create(connectionString);
            database = mongoClient.getDatabase(DB_NAME);
            // Test connection by listing collections
            database.listCollectionNames().first();
            return true;
        } catch (MongoException e) {
            throw new ConnectionException("Could not connect to MongoDB: " + e.getMessage(), e);
        }
    }

    @Override
    public void disconnect() throws ConnectionException {
        try {
            if (mongoClient != null) {
                mongoClient.close();
            }
        } catch (Exception e) {
            throw new ConnectionException("Could not disconnect from MongoDB.", e);
        }
    }
    
    /**
     * Simulates AUTO_INCREMENT by using a counters collection.
     * Atomically increments and returns the next sequence number for a collection.
     * 
     * @param collectionName The name of the sequence (e.g. "books").
     * @return The next integer ID.
     * @throws InsertException if generation fails.
     */
    private int getNextSequence(String collectionName) throws InsertException {
        try {
            MongoCollection<Document> counters = database.getCollection(COUNTERS_COLLECTION);
            
            Bson filter = eq("_id", collectionName + "_id");
            Bson update = Updates.inc("seq", 1);
            
            Document result = counters.findOneAndUpdate(filter, update, 
                new com.mongodb.client.model.FindOneAndUpdateOptions()
                    .returnDocument(com.mongodb.client.model.ReturnDocument.AFTER)
                    .upsert(true));
            
            if (result != null) {
                return result.getInteger("seq");
            } else {
                throw new InsertException("Failed to generate ID for " + collectionName);
            }
        } catch (MongoException e) {
            throw new InsertException("Error generating next sequence ID", e);
        }
    }

    @Override
    public User login(String username, String password) throws SelectException {
        try {
            MongoCollection<Document> users = database.getCollection(USERS_COLLECTION);
            Document userDoc = users.find(Filters.and(eq("username", username), eq("password", password))).first();
            
            if (userDoc != null) {
                return new User(userDoc.getInteger("_id"), userDoc.getString("username"));
            }
            return null;
        } catch (MongoException e) {
            throw new SelectException("Error logging in user: " + username, e);
        }
    }

    /**
     * Helper to fetch a User object by ID.
     */
    private User fetchUser(int userId) {
        if (userId == 0) return null;
        MongoCollection<Document> users = database.getCollection(USERS_COLLECTION);
        Document doc = users.find(eq("_id", userId)).first();
        if (doc != null) {
            return new User(doc.getInteger("_id"), doc.getString("username"));
        }
        return null;
    }

    /**
     * Maps a MongoDB Document to a Book object.
     * Handles manual joins for Authors and Genres (referenced by ID).
     * Handles embedded Reviews.
     */
    private Book mapBook(Document doc) {
        int bookId = doc.getInteger("_id");
        String isbn = doc.getString("isbn");
        String title = doc.getString("title");
        String publisher = doc.getString("publisher");
        
        Book book = new Book(bookId, isbn, title, publisher);
        
        Integer addedById = doc.getInteger("added_by");
        if (addedById != null) {
            book.setAddedBy(fetchUser(addedById));
        }

        List<Integer> authorIds = doc.getList("author_ids", Integer.class);
        if (authorIds != null && !authorIds.isEmpty()) {
            MongoCollection<Document> authorsCol = database.getCollection(AUTHORS_COLLECTION);
            List<Document> authorDocs = authorsCol.find(in("_id", authorIds)).into(new ArrayList<>());
            for (Document aDoc : authorDocs) {
                book.addAuthor(mapAuthor(aDoc));
            }
        }

        List<Integer> genreIds = doc.getList("genre_ids", Integer.class);
        if (genreIds != null && !genreIds.isEmpty()) {
            MongoCollection<Document> genresCol = database.getCollection(GENRES_COLLECTION);
            List<Document> genreDocs = genresCol.find(in("_id", genreIds)).into(new ArrayList<>());
            for (Document gDoc : genreDocs) {
                book.addGenre(new Genre(gDoc.getInteger("_id"), gDoc.getString("name")));
            }
        }

        List<Document> reviewDocs = doc.getList("reviews", Document.class);
        if (reviewDocs != null) {
            for (Document rDoc : reviewDocs) {
                int rating = rDoc.getInteger("rating");
                String text = rDoc.getString("text");
                java.util.Date dateUtil = rDoc.getDate("date");
                java.sql.Date dateSql = new java.sql.Date(dateUtil.getTime());
                
                int userId = rDoc.getInteger("user_id");
                User reviewer = fetchUser(userId); 
                if (reviewer == null) {
                    reviewer = new User(userId, "Unknown");
                }
                
                book.addReview(new Review(book, reviewer, rating, text, dateSql));
            }
        }

        return book;
    }
    
    private Author mapAuthor(Document doc) {
        int id = doc.getInteger("_id");
        String name = doc.getString("name");
        java.util.Date birthDateUtil = doc.getDate("birthdate");
        java.sql.Date birthDateSql = (birthDateUtil != null) ? new java.sql.Date(birthDateUtil.getTime()) : null;
        
        Author author = new Author(id, name, birthDateSql);
        
        Integer addedById = doc.getInteger("added_by");
        if (addedById != null) {
            author.setAddedBy(fetchUser(addedById));
        }
        return author;
    }

    @Override
    public List<Book> findBooksByTitle(String title) throws SelectException {
        try {
            MongoCollection<Document> books = database.getCollection(BOOKS_COLLECTION);
            Pattern pattern = Pattern.compile(".*" + Pattern.quote(title) + ".*", Pattern.CASE_INSENSITIVE);
            List<Document> found = books.find(regex("title", pattern)).into(new ArrayList<>());
            
            return found.stream().map(this::mapBook).collect(Collectors.toList());
        } catch (MongoException e) {
            throw new SelectException("Error finding books by title: " + title, e);
        }
    }

    @Override
    public List<Book> findBooksByIsbn(String isbn) throws SelectException {
        try {
            MongoCollection<Document> books = database.getCollection(BOOKS_COLLECTION);
            List<Document> found = books.find(eq("isbn", isbn)).into(new ArrayList<>());
            return found.stream().map(this::mapBook).collect(Collectors.toList());
        } catch (MongoException e) {
            throw new SelectException("Error finding books by isbn: " + isbn, e);
        }
    }

    /**
     * Finds books by author name.
     * First finds matching authors, then finds books referencing those authors.
     */
    @Override
    public List<Book> findBooksByAuthor(String authorName) throws SelectException {
        try {
            MongoCollection<Document> authors = database.getCollection(AUTHORS_COLLECTION);
            Pattern pattern = Pattern.compile(".*" + Pattern.quote(authorName) + ".*", Pattern.CASE_INSENSITIVE);
            List<Document> foundAuthors = authors.find(regex("name", pattern)).into(new ArrayList<>());
            
            List<Integer> authorIds = foundAuthors.stream()
                .map(doc -> doc.getInteger("_id"))
                .collect(Collectors.toList());
            
            if (authorIds.isEmpty()) {
                return new ArrayList<>();
            }

            MongoCollection<Document> books = database.getCollection(BOOKS_COLLECTION);
            List<Document> foundBooks = books.find(in("author_ids", authorIds)).into(new ArrayList<>());
            
            return foundBooks.stream().map(this::mapBook).collect(Collectors.toList());
        } catch (MongoException e) {
            throw new SelectException("Error finding books by author: " + authorName, e);
        }
    }

    /**
     * Finds books by genre name.
     * First finds the matching genre, then finds books referencing that genre.
     */
    @Override
    public List<Book> findBooksByGenre(String genreName) throws SelectException {
        try {
            MongoCollection<Document> genres = database.getCollection(GENRES_COLLECTION);
            Document foundGenre = genres.find(eq("name", genreName)).first();
            
            if (foundGenre == null) {
                return new ArrayList<>();
            }
            
            int genreId = foundGenre.getInteger("_id");

            MongoCollection<Document> books = database.getCollection(BOOKS_COLLECTION);
            List<Document> foundBooks = books.find(in("genre_ids", genreId)).into(new ArrayList<>());
            
            return foundBooks.stream().map(this::mapBook).collect(Collectors.toList());
        } catch (MongoException e) {
            throw new SelectException("Error finding books by genre: " + genreName, e);
        }
    }

    /**
     * Finds books by rating.
     * Retrieves all books and filters in memory (Note: aggregation would be more efficient for large datasets).
     */
    @Override
    public List<Book> findBooksByRating(int rating) throws SelectException {
        try {
            MongoCollection<Document> books = database.getCollection(BOOKS_COLLECTION);
            List<Document> allBooks = books.find().into(new ArrayList<>());
            
            List<Book> result = new ArrayList<>();
            for (Document doc : allBooks) {
                Book b = mapBook(doc);
                if (b.getRating() >= rating) {
                    result.add(b);
                }
            }
            return result;
            
        } catch (MongoException e) {
            throw new SelectException("Error finding books by rating: " + rating, e);
        }
    }

    /**
     * Adds a new book to the database.
     * Stores author and genre relations as arrays of IDs in the book document.
     */
    @Override
    public void addBook(Book book) throws InsertException {
        try {
            int bookId = getNextSequence(BOOKS_COLLECTION);
            
            List<Integer> authorIds = book.getAuthors().stream()
                .map(Author::getAuthorId)
                .collect(Collectors.toList());
                
            List<Integer> genreIds = book.getGenres().stream()
                .map(Genre::getGenreId)
                .collect(Collectors.toList());
                
            Document doc = new Document("_id", bookId)
                .append("isbn", book.getIsbn())
                .append("title", book.getTitle())
                .append("publisher", book.getPublisher())
                .append("added_by", book.getAddedBy() != null ? book.getAddedBy().getId() : null)
                .append("author_ids", authorIds)
                .append("genre_ids", genreIds)
                .append("reviews", new ArrayList<>());
                
            database.getCollection(BOOKS_COLLECTION).insertOne(doc);
            
        } catch (MongoException e) {
            throw new InsertException("Error adding book: " + book.getTitle(), e);
        }
    }

    @Override
    public void addAuthor(Author author) throws InsertException {
        try {
            int authorId = getNextSequence(AUTHORS_COLLECTION);
            
            Document doc = new Document("_id", authorId)
                .append("name", author.getName())
                .append("birthdate", author.getBirthdate())
                .append("added_by", author.getAddedBy() != null ? author.getAddedBy().getId() : null);
                
            database.getCollection(AUTHORS_COLLECTION).insertOne(doc);
            
        } catch (MongoException e) {
            throw new InsertException("Error adding author: " + author.getName(), e);
        }
    }

    @Override
    public void addGenre(Genre genre) throws InsertException {
        try {
            int genreId = getNextSequence(GENRES_COLLECTION);
            
            Document doc = new Document("_id", genreId)
                .append("name", genre.getName());
                
            database.getCollection(GENRES_COLLECTION).insertOne(doc);
            
        } catch (MongoException e) {
            throw new InsertException("Error adding genre: " + genre.getName(), e);
        }
    }

    /**
     * Adds a review to a book.
     * Reviews are stored as embedded documents within the book document.
     */
    @Override
    public void addReview(Book book, User user, int rating, String reviewText) throws InsertException {
        try {
            Document reviewDoc = new Document("rating", rating)
                .append("text", reviewText)
                .append("date", new java.util.Date())
                .append("user_id", user.getId());
                
            database.getCollection(BOOKS_COLLECTION).updateOne(
                eq("_id", book.getBookId()),
                Updates.push("reviews", reviewDoc)
            );
            
        } catch (MongoException e) {
            throw new InsertException("Error adding review for book: " + book.getTitle(), e);
        }
    }

    @Override
    public List<Author> getAllAuthors() throws SelectException {
        try {
            MongoCollection<Document> authors = database.getCollection(AUTHORS_COLLECTION);
            List<Document> docs = authors.find().sort(eq("name", 1)).into(new ArrayList<>());
            
            return docs.stream().map(this::mapAuthor).collect(Collectors.toList());
        } catch (MongoException e) {
            throw new SelectException("Error fetching all authors", e);
        }
    }

    @Override
    public List<Genre> getAllGenres() throws SelectException {
        try {
            MongoCollection<Document> genres = database.getCollection(GENRES_COLLECTION);
            List<Document> docs = genres.find().sort(eq("name", 1)).into(new ArrayList<>());
            
            List<Genre> result = new ArrayList<>();
            for (Document d : docs) {
                result.add(new Genre(d.getInteger("_id"), d.getString("name")));
            }
            return result;
        } catch (MongoException e) {
            throw new SelectException("Error fetching all genres", e);
        }
    }

    @Override
    public void removeBook(Book book) throws Exception {
        try {
            database.getCollection(BOOKS_COLLECTION).deleteOne(eq("_id", book.getBookId()));
        } catch (MongoException e) {
            throw new Exception("Could not remove book", e);
        }
    }
}
