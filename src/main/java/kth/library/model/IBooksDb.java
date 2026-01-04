package kth.library.model;

import kth.library.model.exceptions.ConnectionException;
import kth.library.model.exceptions.InsertException;
import kth.library.model.exceptions.SelectException;

import java.util.List;

/**
 * This interface declares methods for querying a Books database.
 * Different implementations of this interface handles the connection and
 * queries to a specific DBMS and database, for example a MySQL or a MongoDB
 * database.
 *
 * NB! The methods in the implementation must catch the SQL/MongoDBExceptions thrown
 * by the underlying driver, wrap in a Connection/Insert/SelectException and re-throw the
 * latter exception. This way the interface is the same for both implementations, because the
 * exception type in the method signatures is the same. More info in the mentioned exception classes.
 * 
 * @author anderslm@kth.se
 */
public interface IBooksDb {
    
    /**
     * Connect to the database.
     * @param database url
     * @return true on successful connection
     */
    boolean connect(String database) throws ConnectionException;
    
    void disconnect() throws ConnectionException;
    
    /**
     * Login a user.
     * @return User object if successful, null if not found/wrong password.
     */
    User login(String username, String password) throws SelectException;
    
    List<Book> findBooksByTitle(String title) throws SelectException;

    List<Book> findBooksByIsbn(String isbn) throws SelectException;
    
    List<Book> findBooksByAuthor(String author) throws SelectException;
    
    List<Book> findBooksByGenre(String genre) throws SelectException;
    
    List<Book> findBooksByRating(int rating) throws SelectException;
    
    /**
     * Add a book. The book object should have the addedBy field set if a user is logged in.
     */
    void addBook(Book book) throws InsertException;
    
    /**
     * Add an author to the database. 
     * The author object should have the addedBy field set if a user is logged in.
     */
    void addAuthor(Author author) throws InsertException;

    /**
     * Add a genre to the database.
     */
    void addGenre(Genre genre) throws InsertException;

    /**
     * Add a review (rating + text) for a book by a user.
     */
    void addReview(Book book, User user, int rating, String reviewText) throws InsertException;
    
    // Helper methods to fetch available authors/genres for the "Add Book" dialog
    List<Author> getAllAuthors() throws SelectException;
    List<Genre> getAllGenres() throws SelectException;

    /**
     * Remove a book from the database.
     */
    void removeBook(Book book) throws Exception;
}
