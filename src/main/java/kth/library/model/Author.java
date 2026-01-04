package kth.library.model;

import java.sql.Date;

/**
 * Represents an author in the library system.
 * An author can have written multiple books.
 */
public class Author {
    private final int authorId;
    private final String name;
    private final Date birthdate;
    private User addedBy; // New for VG

    public Author(int authorId, String name, Date birthdate) {
        this.authorId = authorId;
        this.name = name;
        this.birthdate = birthdate;
    }

    public Author(String name, Date birthdate) {
        this(-1, name, birthdate);
    }

    public int getAuthorId() {
        return authorId;
    }

    public String getName() {
        return name;
    }

    public Date getBirthdate() {
        return birthdate;
    }
    
    public User getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(User addedBy) {
        this.addedBy = addedBy;
    }

    @Override
    public String toString() {
        return name;
    }
}
