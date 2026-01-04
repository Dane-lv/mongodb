package kth.library.model;

import kth.library.model.exceptions.ConnectionException;
import kth.library.model.exceptions.InsertException;
import kth.library.model.exceptions.SelectException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Real implementation of IBooksDB that communicates with a MySQL database.
 * Handles all SQL queries and transactions.
 */
public class BooksDbImpl implements IBooksDb {

    private Connection connection;
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String USER = "library_client";
    private static final String PASSWORD = "lib123";
    
    // Common Base Query for books including user info
    private static final String SELECT_BOOKS_BASE = 
        "SELECT b.*, u.username as added_by_username FROM T_Book b LEFT JOIN T_User u ON b.added_by = u.user_id ";

    public BooksDbImpl() {
        try {
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load JDBC driver: " + JDBC_DRIVER, e);
        }
    }

    @Override
    public boolean connect(String databaseUrl) throws ConnectionException {
        try {
            if (connection != null && !connection.isClosed()) {
                return true; // Already connected
            }
            connection = DriverManager.getConnection(databaseUrl, USER, PASSWORD);
            return true;
        } catch (SQLException e) {
            throw new ConnectionException("Could not connect to database: " + databaseUrl, e);
        }
    }

    @Override
    public void disconnect() throws ConnectionException {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new ConnectionException("Could not disconnect from database.", e);
        }
    }

    @Override
    public User login(String username, String password) throws SelectException {
        String sql = "SELECT user_id, username FROM T_User WHERE username = ? AND password = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getInt("user_id"), rs.getString("username"));
                }
            }
            return null;
        } catch (SQLException e) {
            throw new SelectException("Error logging in user: " + username, e);
        }
    }
    
    // --- Helper interface and method for executing search queries ---
    
    @FunctionalInterface
    private interface StatementPreparer {
        void prepare(PreparedStatement stmt) throws SQLException;
    }

    private List<Book> executeSearch(String sql, StatementPreparer preparer, String errorMessage) throws SelectException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            preparer.prepare(stmt);
            try (ResultSet rs = stmt.executeQuery()) {
                return mapBooks(rs);
            }
        } catch (SQLException e) {
            throw new SelectException(errorMessage, e);
        }
    }

    // --- Helper method to map ResultSet to Book list ---
    private List<Book> mapBooks(ResultSet rs) throws SQLException, SelectException {
        List<Book> books = new ArrayList<>();
        while (rs.next()) {
            int id = rs.getInt("book_id");
            String isbn = rs.getString("isbn");
            String title = rs.getString("title");
            String publisher = rs.getString("publisher");
            
            Book book = new Book(id, isbn, title, publisher);
            
            // Set Added By User if present
            int userId = rs.getInt("added_by"); // 0 if null
            if (!rs.wasNull()) {
                String username = rs.getString("added_by_username"); // Alias used in queries
                book.setAddedBy(new User(userId, username));
            }

            // Fetch related data
            fetchAuthorsForBook(book);
            fetchGenresForBook(book);
            fetchReviewsForBook(book);
            
            books.add(book);
        }
        return books;
    }
    
    private void fetchAuthorsForBook(Book book) throws SQLException {
        String sql = "SELECT a.author_id, a.name, a.birthdate, a.added_by, u.username as added_by_username " +
                     "FROM T_Author a " +
                     "JOIN T_Book_Author ba ON a.author_id = ba.author_id " +
                     "LEFT JOIN T_User u ON a.added_by = u.user_id " +
                     "WHERE ba.book_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, book.getBookId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("author_id");
                    String name = rs.getString("name");
                    Date birthDate = rs.getDate("birthdate");
                    Author author = new Author(id, name, birthDate);
                    
                    int userId = rs.getInt("added_by");
                    if (!rs.wasNull()) {
                        author.setAddedBy(new User(userId, rs.getString("added_by_username")));
                    }
                    
                    book.addAuthor(author);
                }
            }
        }
    }
    
    private void fetchGenresForBook(Book book) throws SQLException {
        String sql = "SELECT g.genre_id, g.name FROM T_Genre g " +
                     "JOIN T_Book_Genre bg ON g.genre_id = bg.genre_id " +
                     "WHERE bg.book_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, book.getBookId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("genre_id");
                    String name = rs.getString("name");
                    book.addGenre(new Genre(id, name));
                }
            }
        }
    }
    
    private void fetchReviewsForBook(Book book) throws SQLException {
        String sql = "SELECT r.rating, r.review_text, r.review_date, r.user_id, u.username " +
                     "FROM T_Review r " +
                     "JOIN T_User u ON r.user_id = u.user_id " +
                     "WHERE r.book_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, book.getBookId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int rating = rs.getInt("rating");
                    String text = rs.getString("review_text");
                    Date date = rs.getDate("review_date");
                    User user = new User(rs.getInt("user_id"), rs.getString("username"));
                    book.addReview(new Review(book, user, rating, text, date));
                }
            }
        }
    }

    @Override
    public List<Book> findBooksByTitle(String title) throws SelectException {
        String sql = SELECT_BOOKS_BASE + "WHERE b.title LIKE ?";
        return executeSearch(sql, stmt -> stmt.setString(1, "%" + title + "%"), "Error finding books by title: " + title);
    }

    @Override
    public List<Book> findBooksByIsbn(String isbn) throws SelectException {
        String sql = SELECT_BOOKS_BASE + "WHERE b.isbn = ?";
        return executeSearch(sql, stmt -> stmt.setString(1, isbn.trim()), "Error finding books by ISBN: " + isbn);
    }

    @Override
    public List<Book> findBooksByAuthor(String author) throws SelectException {
        String sql = "SELECT DISTINCT b.*, u.username as added_by_username FROM T_Book b " +
                     "LEFT JOIN T_User u ON b.added_by = u.user_id " +
                     "JOIN T_Book_Author ba ON b.book_id = ba.book_id " +
                     "JOIN T_Author a ON ba.author_id = a.author_id " +
                     "WHERE a.name LIKE ?";
        return executeSearch(sql, stmt -> stmt.setString(1, "%" + author + "%"), "Error finding books by author: " + author);
    }

    @Override
    public List<Book> findBooksByGenre(String genre) throws SelectException {
        String sql = "SELECT DISTINCT b.*, u.username as added_by_username FROM T_Book b " +
                     "LEFT JOIN T_User u ON b.added_by = u.user_id " +
                     "JOIN T_Book_Genre bg ON b.book_id = bg.book_id " +
                     "JOIN T_Genre g ON bg.genre_id = g.genre_id " +
                     "WHERE g.name = ?";
        return executeSearch(sql, stmt -> stmt.setString(1, genre), "Error finding books by genre: " + genre);
    }

    @Override
    public List<Book> findBooksByRating(int rating) throws SelectException {
        String sql = "SELECT b.*, u.username as added_by_username FROM T_Book b " +
                     "LEFT JOIN T_User u ON b.added_by = u.user_id " +
                     "JOIN T_Review r ON b.book_id = r.book_id " +
                     "GROUP BY b.book_id " +
                     "HAVING AVG(r.rating) >= ?";
        return executeSearch(sql, stmt -> stmt.setInt(1, rating), "Error finding books by rating: " + rating);
    }

    @Override
    public void addBook(Book book) throws InsertException {
        String insertBookSql = "INSERT INTO T_Book (isbn, title, publisher, added_by) VALUES (?, ?, ?, ?)";
        String insertAuthorRelSql = "INSERT INTO T_Book_Author (book_id, author_id) VALUES (?, ?)";
        String insertGenreRelSql = "INSERT INTO T_Book_Genre (book_id, genre_id) VALUES (?, ?)";
        
        try {
            connection.setAutoCommit(false);
            
            int bookId;
            try (PreparedStatement stmt = connection.prepareStatement(insertBookSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, book.getIsbn());
                stmt.setString(2, book.getTitle());
                stmt.setString(3, book.getPublisher());
                
                if (book.getAddedBy() != null) {
                    stmt.setInt(4, book.getAddedBy().getId());
                } else {
                    stmt.setNull(4, Types.INTEGER);
                }
                
                stmt.executeUpdate(); // for INSERT, UPDATE, DELETE
                
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        bookId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Creating book failed, no ID obtained.");
                    }
                }
            }
            
            // Insert Author Relations
            try (PreparedStatement stmt = connection.prepareStatement(insertAuthorRelSql)) {
                for (Author author : book.getAuthors()) {
                    stmt.setInt(1, bookId);
                    stmt.setInt(2, author.getAuthorId());
                    stmt.executeUpdate();
                }
            }
            
            // Insert Genre Relations
            try (PreparedStatement stmt = connection.prepareStatement(insertGenreRelSql)) {
                for (Genre genre : book.getGenres()) {
                    stmt.setInt(1, bookId);
                    stmt.setInt(2, genre.getGenreId());
                    stmt.executeUpdate();
                }
            }
            
            connection.commit();
            
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                // Ignore rollback error
            }
            throw new InsertException("Error adding book: " + book.getTitle(), e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                // Ignore
            }
        }
    }

    @Override
    public void addAuthor(Author author) throws InsertException {
        
    }

    @Override
    public void addGenre(Genre genre) throws InsertException {
        
    }

    @Override
    public void addReview(Book book, User user, int rating, String reviewText) throws InsertException {
        String sql = "INSERT INTO T_Review (book_id, user_id, rating, review_text, review_date) VALUES (?, ?, ?, ?, CURRENT_DATE)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, book.getBookId());
            stmt.setInt(2, user.getId());
            stmt.setInt(3, rating);
            if (reviewText != null) {
                stmt.setString(4, reviewText);
            } else {
                stmt.setNull(4, Types.VARCHAR);
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new InsertException("Error adding review for book: " + book.getTitle(), e);
        }
    }

    @Override
    public List<Author> getAllAuthors() throws SelectException {
        String sql = "SELECT * FROM T_Author ORDER BY name";
        List<Author> authors = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                authors.add(new Author(rs.getInt("author_id"), rs.getString("name"), rs.getDate("birthdate")));
            }
            return authors;
        } catch (SQLException e) {
            throw new SelectException("Error fetching all authors", e);
        }
    }

    @Override
    public List<Genre> getAllGenres() throws SelectException {
        String sql = "SELECT * FROM T_Genre ORDER BY name";
        List<Genre> genres = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                genres.add(new Genre(rs.getInt("genre_id"), rs.getString("name")));
            }
            return genres;
        } catch (SQLException e) {
            throw new SelectException("Error fetching all genres", e);
        }
    }

    @Override
    public void removeBook(Book book) throws Exception {
        String sql = "DELETE FROM T_Book WHERE book_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, book.getBookId());
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new Exception("Book not found");
            }
        } catch (SQLException e) {
            throw new Exception("Could not remove book", e);
        }
    }
}
