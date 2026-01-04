package kth.library.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a book in the library system.
 * A book can have multiple authors and genres
 * and can be reviewed by multiple users.
 */
public class Book {

    private final int bookId;
    private final String isbn;
    private final String title;
    private final String publisher;
    

    private User addedBy; 
    private final List<Review> reviews;
    
    private final List<Author> authors;
    private final List<Genre> genres;

    public Book(int bookId, String isbn, String title, String publisher) {
        this.bookId = bookId;
        this.isbn = isbn;
        this.title = title;
        this.publisher = publisher;
        this.authors = new ArrayList<>();
        this.genres = new ArrayList<>();
        this.reviews = new ArrayList<>();
    }

    public Book(String isbn, String title, String publisher) {
        this(-1, isbn, title, publisher);
    }

    public int getBookId() {
        return bookId;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public String getPublisher() {
        return publisher;
    }
    
    public User getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(User addedBy) {
        this.addedBy = addedBy;
    }

    /**
     * Calculates average rating from reviews.
     * @return Average rating or 0.0 if no reviews.
     */
    public double getRating() {
        if (reviews.isEmpty()) {
            return 0.0;
        }
        int sum = 0;
        for (Review r : reviews) {
            sum += r.getRating();
        }
        return (double) sum / reviews.size();
    }

    public List<Review> getReviews() {
        return reviews;
    }
    
    public void addReview(Review review) {
        reviews.add(review);
    }
    
    public void setReviews(List<Review> reviews) {
        this.reviews.clear();
        if (reviews != null) {
            this.reviews.addAll(reviews);
        }
    }

    public List<Author> getAuthors() {
        return authors;
    }
    
    public void addAuthor(Author author) {
        authors.add(author);
    }
    
    public void setAuthors(List<Author> authors) {
        this.authors.clear();
        if (authors != null) {
            this.authors.addAll(authors);
        }
    }

    public List<Genre> getGenres() {
        return genres;
    }
    
    public void addGenre(Genre genre) {
        genres.add(genre);
    }
    
    public void setGenres(List<Genre> genres) {
        this.genres.clear();
        if (genres != null) {
            this.genres.addAll(genres);
        }
    }

    @Override
    public String toString() {
        return title + ", " + isbn + ", " + publisher;
    }
}
