package kth.library;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import kth.library.model.IBooksDb;
import kth.library.model.BooksDbImpl;
import kth.library.model.BooksDbMongoImpl;
import kth.library.view.BooksPane;

import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws IOException {
        // IBooksDb booksDb = new BooksDbImpl(); // MySQL implementation
        IBooksDb booksDb = new BooksDbMongoImpl(); // MongoDB implementation
        
        BooksPane booksPane = new BooksPane(booksDb); // also creates a controller

        // Connect to the db
        try {
            // The connect string is handled internally in BooksDbMongoImpl for this lab, 
            // but we pass a dummy string or the real one if we wanted to fully support it.
            booksDb.connect("mongodb://localhost:27017/library_db");
        } catch (Exception e) {
            System.err.println("Failed to connect to database: " + e.getMessage());
            // Usually show an alert here, but for now console is fine
        }

        Scene scene = new Scene(booksPane, 800, 600);
        primaryStage.setTitle("Books Database Client (MongoDB)");
        // add an exit handler to the stage (X)
        primaryStage.setOnCloseRequest(event -> {
            try {
                booksDb.disconnect();
            } catch (Exception e) {
            }
        });
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
