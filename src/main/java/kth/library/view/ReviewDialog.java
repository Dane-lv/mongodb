package kth.library.view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

import java.util.stream.IntStream;

public class ReviewDialog extends Dialog<Pair<Integer, String>> {

    public ReviewDialog(String bookTitle) {
        this.setTitle("Rate & Review");
        this.setHeaderText("Rate and review: " + bookTitle);

        ButtonType submitButtonType = new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE);
        this.getDialogPane().getButtonTypes().addAll(submitButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<Integer> ratingBox = new ComboBox<>();
        IntStream.rangeClosed(1, 5).forEach(ratingBox.getItems()::add);
        ratingBox.setValue(5); // Default

        TextArea reviewText = new TextArea();
        reviewText.setPromptText("Write your review here...");
        reviewText.setPrefRowCount(3);
        reviewText.setWrapText(true);

        grid.add(new Label("Rating:"), 0, 0);
        grid.add(ratingBox, 1, 0);
        grid.add(new Label("Review:"), 0, 1);
        grid.add(reviewText, 1, 1);

        this.getDialogPane().setContent(grid);

        Platform.runLater(ratingBox::requestFocus);

        this.setResultConverter(dialogButton -> {
            if (dialogButton == submitButtonType) {
                return new Pair<>(ratingBox.getValue(), reviewText.getText());
            }
            return null;
        });
    }
}

