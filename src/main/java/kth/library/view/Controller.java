package kth.library.view;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.util.Pair;
import kth.library.model.Book;
import kth.library.model.IBooksDb;
import kth.library.model.SearchMode;
import kth.library.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static javafx.scene.control.Alert.AlertType.*;

/**
 * The controller is responsible for handling user requests and update the view
 * (and in some cases the model).
 *
 * @author anderslm@kth.se
 */
public class Controller {

    private final BooksPane booksView; // view
    private final IBooksDb booksDb; // model
    private User currentUser; // The currently logged in user (null if anonymous)

    public Controller(IBooksDb booksDb, BooksPane booksView) {
        this.booksDb = booksDb;
        this.booksView = booksView;
    }
    
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    public User getCurrentUser() {
        return currentUser;
    }

    protected void onLogin(String username, String password) {
        new Thread(() -> {
            try {
                User user = booksDb.login(username, password);
                Platform.runLater(() -> {
                    if (user != null) {
                        currentUser = user;
                        booksView.showAlertAndWait("Welcome " + user.getUsername(), INFORMATION);
                        booksView.updateMenuState(true);
                    } else {
                        booksView.showAlertAndWait("Login failed. Wrong username or password.", ERROR);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> booksView.showAlertAndWait("Login error: " + e.getMessage(), ERROR));
            }
        }).start();
    }
    
    protected void onLogout() {
        currentUser = null;
        booksView.updateMenuState(false);
        booksView.showAlertAndWait("You have been logged out.", INFORMATION);
    }

    protected void onSearchSelected(String searchFor, SearchMode mode) {
        new Thread(() -> {
            try {
                if (searchFor != null && !searchFor.isEmpty()) {
                    List<Book> result = null;
                    switch (mode) {
                        case Title:
                            result = booksDb.findBooksByTitle(searchFor );
                            break;
                        case ISBN:
                            result = booksDb.findBooksByIsbn(searchFor);
                            break;
                        case Author:
                            result = booksDb.findBooksByAuthor(searchFor);
                            break;
                        case Genre:
                            result = booksDb.findBooksByGenre(searchFor);
                            break;
                        case Rating:
                            // For rating, we expect an integer
                            try {
                                int rating = Integer.parseInt(searchFor);
                                result = booksDb.findBooksByRating(rating);
                            } catch (NumberFormatException e) {
                                Platform.runLater(() -> booksView.showAlertAndWait("Rating must be a number", WARNING));
                                return;
                            }
                            break;
                        default:
                            result= new ArrayList<>();
                    }
                    final List<Book> finalResult = result;
                    Platform.runLater(() -> {
                        if (finalResult == null || finalResult.isEmpty()) {
                            booksView.showAlertAndWait("No results found.", INFORMATION);
                        } else {
                            booksView.displayBooks(finalResult);
                        }
                    });
                } else {
                    Platform.runLater(() -> booksView.showAlertAndWait("Enter a search string!", WARNING));
                }
            } catch (Exception e) {
                Platform.runLater(() -> booksView.showAlertAndWait("Database error: " + e.getMessage(), ERROR));
            }
        }).start();
    }
    
    protected void onRateBookSelected(Book book) {
        if (currentUser == null) {
            booksView.showAlertAndWait("You must be logged in to rate books.", WARNING);
            return;
        }

        ReviewDialog dialog = new ReviewDialog(book.getTitle());
        Optional<Pair<Integer, String>> result = dialog.showAndWait();
        
        result.ifPresent(review -> {
            new Thread(() -> {
                try {
                    booksDb.addReview(book, currentUser, review.getKey(), review.getValue());
                    Platform.runLater(() -> {
                        booksView.showAlertAndWait("Review added!", INFORMATION);
                        // Refresh view if needed?
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> booksView.showAlertAndWait("Error adding review: " + e.getMessage(), ERROR));
                }
            }).start();
        });
    }
    
    protected void onAddBookSelected() {
        if (currentUser == null) {
            booksView.showAlertAndWait("You must be logged in to add books.", WARNING);
            return;
        }
    
        new Thread(() -> {
            try {
                // Fetch available authors and genres first
                List<kth.library.model.Author> authors = booksDb.getAllAuthors();
                List<kth.library.model.Genre> genres = booksDb.getAllGenres();

                Platform.runLater(() -> {
                    AddBookDialog dialog = new AddBookDialog(authors, genres);
                    Optional<Book> result = dialog.showAndWait();
                    
                    result.ifPresent(newBook -> {
                        // Set the user who is adding the book
                        newBook.setAddedBy(currentUser);
                        
                        // Note: The dialog creates Author objects, but those are "existing" authors selected from list?
                        // Wait, if AddBookDialog allows creating NEW authors, we need to handle that.
                        // The requirement says "Only give possibility to add already known authors".
                        // So authors are selected from the list. No new authors created here.
                        // But what about the author object? It needs to be valid.
                        // Assuming AddBookDialog returns Book with valid Author objects from the list.
                        
                        new Thread(() -> {
                            try {
                                booksDb.addBook(newBook);
                                Platform.runLater(() -> {
                                    booksView.showAlertAndWait("Book added successfully!", INFORMATION);
                                });
                            } catch (Exception e) {
                                Platform.runLater(() -> booksView.showAlertAndWait("Error adding book: " + e.getMessage(), ERROR));
                            }
                        }).start();
                    });
                });
            } catch (Exception e) {
                Platform.runLater(() -> booksView.showAlertAndWait("Could not fetch authors/genres: " + e.getMessage(), ERROR));
            }
        }).start();
    }

    protected void onRemoveBookSelected() {
        if (currentUser == null) {
            booksView.showAlertAndWait("You must be logged in to remove books.", WARNING);
            return;
        }

        Book selected = booksView.getSelectedBook();
        if (selected == null) {
            booksView.showAlertAndWait("No book selected", WARNING);
            return;
        }

        new Thread(() -> {
            try {
                booksDb.removeBook(selected);
                Platform.runLater(() -> {
                    booksView.showAlertAndWait("Book removed", INFORMATION);
                    // Trigger a re-search or refresh (not fully implemented in view yet, but logic is sound)
                    // For now user has to search again to see it gone
                });
            } catch (Exception e) {
                Platform.runLater(() -> booksView.showAlertAndWait("Error: " + e.getMessage(), ERROR));
            }
        }).start();
    }
}
