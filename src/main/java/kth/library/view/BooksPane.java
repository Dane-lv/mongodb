package kth.library.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
import kth.library.model.Book;
import kth.library.model.IBooksDb;
import kth.library.model.SearchMode;
import kth.library.model.Review;

import java.util.List;
import java.util.Optional;

/**
 * The main pane for the view, extending VBox and including the menus. An
 * internal BorderPane holds the TableView for books and a search utility.
 *
 * @author anderslm@kth.se
 */
public class BooksPane extends VBox {

    private TableView<Book> booksTable;
    private ObservableList<Book> booksInTable; // the data backing the table view

    private ComboBox<SearchMode> searchModeBox;
    private TextField searchField;
    private Button searchButton;

    private MenuBar menuBar;
    
    private MenuItem addItem;
    private MenuItem removeItem; // Not implemented but present in menu
    private MenuItem updateItem; // Rate book
    private MenuItem loginItem;
    private MenuItem logoutItem;

    public BooksPane(IBooksDb booksDb) {
        final Controller controller = new Controller(booksDb, this);
        this.init(controller);
    }

    /**
     * Display a new set of books, e.g. from a database select, in the
     * booksTable table view.
     *
     * @param books the books to display
     */
    public void displayBooks(List<Book> books) {
        booksInTable.clear();
        booksInTable.addAll(books);
    }

    /**
     * Notify user on input error or exceptions.
     *
     * @param msg  the message
     * @param type types: INFORMATION, WARNING et c.
     */
    public void showAlertAndWait(String msg, Alert.AlertType type) {
        // types: INFORMATION, WARNING et c.
        Alert alert = new Alert(type, msg);
        alert.showAndWait();
    }
    
    public void updateMenuState(boolean isLoggedIn) {
        addItem.setDisable(!isLoggedIn);
        removeItem.setDisable(!isLoggedIn);
        updateItem.setDisable(!isLoggedIn); // Cannot rate if not logged in
        
        loginItem.setVisible(!isLoggedIn);
        logoutItem.setVisible(isLoggedIn);
    }

    void init(Controller controller) {

        booksInTable = FXCollections.observableArrayList();

        // init views and event handlers
        initBooksTable();
        initSearchView(controller);
        initMenus(controller);

        FlowPane bottomPane = new FlowPane();
        bottomPane.setHgap(10);
        bottomPane.setPadding(new Insets(10, 10, 10, 10));
        bottomPane.getChildren().addAll(searchModeBox, searchField, searchButton);

        BorderPane mainPane = new BorderPane();
        mainPane.setCenter(booksTable);
        mainPane.setBottom(bottomPane);
        mainPane.setPadding(new Insets(10, 10, 10, 10));

        this.getChildren().addAll(menuBar, mainPane);
        VBox.setVgrow(mainPane, Priority.ALWAYS);
    }

    private void initBooksTable() {
        booksTable = new TableView<>();
        booksTable.setEditable(false); // don't allow user updates (yet)
        booksTable.setPlaceholder(new Label("No rows to display"));

        // define columns
        TableColumn<Book, String> titleCol = new TableColumn<>("Title");
        TableColumn<Book, String> isbnCol = new TableColumn<>("ISBN");
        TableColumn<Book, String> publisherCol = new TableColumn<>("Publisher");
        TableColumn<Book, Double> ratingCol = new TableColumn<>("Rating"); 
        
        booksTable.getColumns().addAll(titleCol, isbnCol, publisherCol, ratingCol);
        // give title column some extra space
        titleCol.prefWidthProperty().bind(booksTable.widthProperty().multiply(0.5));

        // define how to fill data for each cell
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        isbnCol.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        publisherCol.setCellValueFactory(new PropertyValueFactory<>("publisher"));
        ratingCol.setCellValueFactory(new PropertyValueFactory<>("rating"));
        
        // Format rating to 1 decimal
        ratingCol.setCellFactory(tc -> new TableCell<Book, Double>() {
            @Override
            protected void updateItem(Double rating, boolean empty) {
                super.updateItem(rating, empty);
                if (empty || rating == null) {
                    setText(null);
                } else {
                    setText(String.format("%.1f", rating));
                }
            }
        });

        // associate the table view with the data
        booksTable.setItems(booksInTable);
    }

    private void initSearchView(Controller controller) {
        searchField = new TextField();
        searchField.setPromptText("Search for...");
        searchModeBox = new ComboBox<>();
        searchModeBox.getItems().addAll(SearchMode.values());
        searchModeBox.setValue(SearchMode.Title);
        searchButton = new Button("Search");

        // event handling (dispatch to controller)
        searchButton.setOnAction(event -> {
            String searchFor = searchField.getText();
            SearchMode mode = searchModeBox.getValue();
            controller.onSearchSelected(searchFor, mode);
        });
    }

    private void initMenus(Controller controller) {

        Menu fileMenu = new Menu("File");
        MenuItem exitItem = new MenuItem("Exit");
        MenuItem connectItem = new MenuItem("Connect to Db"); // Not hooked up yet
        MenuItem disconnectItem = new MenuItem("Disconnect"); // Not hooked up yet
        loginItem = new MenuItem("Log in");
        logoutItem = new MenuItem("Log out");
        
        fileMenu.getItems().addAll(loginItem, logoutItem, new SeparatorMenuItem(), exitItem);
        
        exitItem.setOnAction(e -> System.exit(0));
        
        loginItem.setOnAction(e -> {
            LoginDialog dialog = new LoginDialog();
            Optional<Pair<String, String>> result = dialog.showAndWait();
            result.ifPresent(creds -> controller.onLogin(creds.getKey(), creds.getValue()));
        });
        
        logoutItem.setOnAction(e -> controller.onLogout());
        logoutItem.setVisible(false); // Default hidden

        Menu searchMenu = new Menu("Search");
        MenuItem titleItem = new MenuItem("Title");
        MenuItem isbnItem = new MenuItem("ISBN");
        MenuItem authorItem = new MenuItem("Author");
        MenuItem genreItem = new MenuItem("Genre");
        MenuItem ratingItem = new MenuItem("Rating");
        searchMenu.getItems().addAll(titleItem, isbnItem, authorItem, genreItem, ratingItem);
        
        // Bind menu items to change the combo box selection
        titleItem.setOnAction(e -> searchModeBox.setValue(SearchMode.Title));
        isbnItem.setOnAction(e -> searchModeBox.setValue(SearchMode.ISBN));
        authorItem.setOnAction(e -> searchModeBox.setValue(SearchMode.Author));
        genreItem.setOnAction(e -> searchModeBox.setValue(SearchMode.Genre));
        ratingItem.setOnAction(e -> searchModeBox.setValue(SearchMode.Rating));

        Menu manageMenu = new Menu("Manage");
        addItem = new MenuItem("Add Book");
        removeItem = new MenuItem("Remove Book");
        updateItem = new MenuItem("Rate & Review");
        MenuItem detailsItem = new MenuItem("Show Details"); 
        manageMenu.getItems().addAll(addItem, removeItem, updateItem, new SeparatorMenuItem(), detailsItem);
        
        // Initial state: Disabled until login
        addItem.setDisable(true);
        removeItem.setDisable(true);
        updateItem.setDisable(true);

        menuBar = new MenuBar();
        menuBar.getMenus().addAll(fileMenu, searchMenu, manageMenu);

        // Event handlers
        updateItem.setOnAction(e -> {
            Book selected = booksTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                controller.onRateBookSelected(selected);
            } else {
                showAlertAndWait("No book selected", Alert.AlertType.WARNING);
            }
        });
        
        detailsItem.setOnAction(e -> {
            Book selected = booksTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showAlertAndWait(formatBookDetails(selected), Alert.AlertType.INFORMATION);
            } else {
                showAlertAndWait("No book selected", Alert.AlertType.WARNING);
            }
        });
        
        addItem.setOnAction(e -> {
            controller.onAddBookSelected();
        });
        
        removeItem.setOnAction(e -> {
            controller.onRemoveBookSelected();
        });
    }
    
    private String formatBookDetails(Book book) {
        StringBuilder sb = new StringBuilder();
        sb.append("Title: ").append(book.getTitle()).append("\n");
        sb.append("ISBN: ").append(book.getIsbn()).append("\n");
        sb.append("Publisher: ").append(book.getPublisher()).append("\n");
        sb.append("Added by: ").append(book.getAddedBy() != null ? book.getAddedBy().getUsername() : "Unknown").append("\n\n");
        
        sb.append("Authors:\n");
        if (book.getAuthors().isEmpty()) {
            sb.append(" - (None listed)\n");
        } else {
            for (kth.library.model.Author a : book.getAuthors()) {
                sb.append(" - ").append(a.getName());
                if (a.getAddedBy() != null) {
                    sb.append(" (Added by: ").append(a.getAddedBy().getUsername()).append(")");
                }
                sb.append("\n");
            }
        }
        
        sb.append("\nGenres:\n");
        if (book.getGenres().isEmpty()) {
            sb.append(" - (None listed)\n");
        } else {
            for (kth.library.model.Genre g : book.getGenres()) {
                sb.append(" - ").append(g.getName()).append("\n");
            }
        }
        
        sb.append("\nReviews:\n");
        if (book.getReviews().isEmpty()) {
            sb.append(" - No reviews yet.\n");
        } else {
            for (Review r : book.getReviews()) {
                sb.append(" - ").append(r.toString());
                if (r.getReviewText() != null && !r.getReviewText().isEmpty()) {
                    sb.append("\n   \"").append(r.getReviewText()).append("\"\n");
                } else {
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }
    
    public Book getSelectedBook() {
        return booksTable.getSelectionModel().getSelectedItem();
    }
}
