/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kth.library.model;

import kth.library.model.exceptions.ConnectionException;
import kth.library.model.exceptions.InsertException;
import kth.library.model.exceptions.SelectException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A mock implementation of the IBooksDB interface to demonstrate how to
 * use it together with the user interface.
 * <p>
 * Your implementation must access a real database.
 * @author anderslm@kth.se
 */
public class IBooksDbMockImpl implements IBooksDb {

    private final List<Book> books; // the mock "database"
    private final List<Author> authors; // mock authors
    private final List<Genre> genres; // mock genres
    private final List<User> users; // mock users

    public IBooksDbMockImpl() {
        books = new ArrayList<>(Arrays.asList(DATA));
        authors = new ArrayList<>();
        genres = new ArrayList<>();
        users = new ArrayList<>();
        users.add(new User(1, "admin")); // Add a mock user
        setupMockData();
    }
    
    private void setupMockData() {
        // Create some mock authors and genres and link them to books
        Author rowling = new Author(1, "J.K. Rowling", null);
        Author tolkien = new Author(2, "J.R.R. Tolkien", null);
        Author martin = new Author(3, "George R.R. Martin", null);
        authors.addAll(Arrays.asList(rowling, tolkien, martin));
        
        Genre fantasy = new Genre(1, "Fantasy");
        Genre adventure = new Genre(2, "Adventure");
        Genre drama = new Genre(3, "Drama");
        genres.addAll(Arrays.asList(fantasy, adventure, drama));
        
        // Link to books (simplified for mock)
        books.get(0).addAuthor(rowling); // Databases Illuminated -> Rowling (fake)
        books.get(0).addGenre(fantasy);
        
        books.get(1).addAuthor(tolkien);
        books.get(1).addGenre(adventure);
        
        // Just add some random data to others so searches work
        for(int i = 2; i < books.size(); i++) {
            books.get(i).addAuthor(martin);
            books.get(i).addGenre(drama);
        }
    }

    @Override
    public boolean connect(String database) throws ConnectionException {
        // mock implementation
        return true;
    }

    @Override
    public void disconnect() throws ConnectionException {
        // mock implementation
    }

    @Override
    public User login(String username, String password) throws SelectException {
        // Mock login: accept any password for the mock user "admin"
        for (User u : users) {
            if (u.getUsername().equalsIgnoreCase(username)) {
                return u;
            }
        }
        return null;
    }

    @Override
    public List<Book> findBooksByTitle(String title)
            throws SelectException {
        List<Book> result = new ArrayList<>();
        title = title.trim().toLowerCase();
        for (Book book : books) {
            if (book.getTitle().toLowerCase().contains(title)) {
                result.add(book);
            }
        }
        return result;
    }

    @Override
    public List<Book> findBooksByIsbn(String isbn) throws SelectException {
        List<Book> result = new ArrayList<>();
        isbn = isbn.trim().toLowerCase();
        for (Book book : books) {
            if (book.getIsbn().toLowerCase().equals(isbn)) { // exact match
                result.add(book);
            }
        }
        return result;
    }
    
    @Override
    public List<Book> findBooksByAuthor(String authorName) throws SelectException {
        List<Book> result = new ArrayList<>();
        authorName = authorName.trim().toLowerCase();
        for (Book book : books) {
            for (Author author : book.getAuthors()) {
                if (author.getName().toLowerCase().contains(authorName)) {
                    result.add(book);
                    break; // Found match for this book
                }
            }
        }
        return result;
    }

    @Override
    public List<Book> findBooksByGenre(String genreName) throws SelectException {
        List<Book> result = new ArrayList<>();
        genreName = genreName.trim().toLowerCase();
        for (Book book : books) {
            for (Genre genre : book.getGenres()) {
                if (genre.getName().toLowerCase().equalsIgnoreCase(genreName)) {
                    result.add(book);
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public List<Book> findBooksByRating(int rating) throws SelectException {
        List<Book> result = new ArrayList<>();
        for (Book book : books) {
            if (book.getRating() >= rating) {
                result.add(book);
            }
        }
        return result;
    }

    @Override
    public void addBook(Book book) throws InsertException {
        books.add(book);
    }
    
    @Override
    public void addAuthor(Author author) throws InsertException {
        authors.add(author);
    }

    @Override
    public void addGenre(Genre genre) throws InsertException {
        genres.add(genre);
    }

    @Override
    public void addReview(Book book, User user, int rating, String reviewText) throws InsertException {
        // Find the book in our mock list and add the review
        for (Book b : books) {
            if (b.getBookId() == book.getBookId()) {
                // In a real app we'd create a Review object here, but for mock we might just print
                // or actually adding it if Book is mutable and we want to test memory state
                b.addReview(new Review(b, user, rating, reviewText, new java.sql.Date(System.currentTimeMillis())));
                return;
            }
        }
    }

    @Override
    public List<Author> getAllAuthors() throws SelectException {
        return new ArrayList<>(authors);
    }

    @Override
    public List<Genre> getAllGenres() throws SelectException {
        return new ArrayList<>(genres);
    }

    @Override
    public void removeBook(Book book) throws Exception {
        
    }

    private static final Book[] DATA = {
            new Book(1, "123456789", "Databases Illuminated", "Cathy Ricardo"),
            new Book(2, "234567891", "Dark Databases", "Someone"),
            new Book(3, "456789012", "The buried giant", "Kazuo Ishiguro"),
            new Book(4, "567890123", "Never let me go", "Kazuo Ishiguro"),
            new Book(5, "678901234", "The remains of the day", "Kazuo Ishiguro"),
            new Book(6, "234567890", "Alias Grace", "Margaret Atwood"),
            new Book(7, "345678911", "The handmaids tale", "Margaret Atwood"),
            new Book(8, "345678901", "Shuggie Bain", "Douglas Stuart"),
            new Book(9, "345678912", "Microserfs", "Douglas Coupland"),
    };
}
