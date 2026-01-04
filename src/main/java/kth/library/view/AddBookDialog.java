package kth.library.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;
import kth.library.model.Author;
import kth.library.model.Book;
import kth.library.model.Genre;

import java.util.ArrayList;
import java.util.List;

public class AddBookDialog extends Dialog<Book> {

    private final TextField isbnField;
    private final TextField titleField;
    private final TextField publisherField;
    private final ListView<Author> authorsListView;
    private final ListView<Genre> genresListView;

    public AddBookDialog(List<Author> availableAuthors, List<Genre> availableGenres) {
        this.setTitle("Add New Book");
        this.setHeaderText("Enter book details and select authors/genres.");

        // Set the button types.
        ButtonType loginButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        this.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        // Create the labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        isbnField = new TextField();
        isbnField.setPromptText("ISBN");
        titleField = new TextField();
        titleField.setPromptText("Title");
        publisherField = new TextField();
        publisherField.setPromptText("Publisher");

        authorsListView = new ListView<>();
        authorsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        authorsListView.getItems().addAll(availableAuthors);
        authorsListView.setPrefHeight(150);

        genresListView = new ListView<>();
        genresListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        genresListView.getItems().addAll(availableGenres);
        genresListView.setPrefHeight(150);

        grid.add(new Label("ISBN:"), 0, 0);
        grid.add(isbnField, 1, 0);
        grid.add(new Label("Title:"), 0, 1);
        grid.add(titleField, 1, 1);
        grid.add(new Label("Publisher:"), 0, 2);
        grid.add(publisherField, 1, 2);
        
        grid.add(new Label("Authors:"), 0, 3);
        grid.add(authorsListView, 1, 3);
        
        grid.add(new Label("Genres:"), 0, 4);
        grid.add(genresListView, 1, 4);

        this.getDialogPane().setContent(grid);

        // Convert the result to a book-object when the add button is clicked.
        this.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                String isbn = isbnField.getText();  
                String title = titleField.getText();
                String publisher = publisherField.getText();
                
                if (isbn.isEmpty() || title.isEmpty() || publisher.isEmpty()) {
                    return null; 
                }
                
                Book newBook = new Book(isbn, title, publisher);
                newBook.setAuthors(new ArrayList<>(authorsListView.getSelectionModel().getSelectedItems()));
                newBook.setGenres(new ArrayList<>(genresListView.getSelectionModel().getSelectedItems()));
                return newBook;
            }
            return null;
        });
    }
}

