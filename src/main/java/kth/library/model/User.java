package kth.library.model;

/**
 * Represents a user in the library system.
 * A user can review and rate books, and can add books to the system.
 */
public class User {
    private final int id;
    private final String username;

    public User(int id, String username) {
        this.id = id;
        this.username = username;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String toString() {
        return username;
    }
}

