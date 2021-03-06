package gui;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.AnalystService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Controller for {@code analyser.fxml}
 *
 * @see AnalystService
 * @see TextFlow
 * @see Text
 */
public class Controller implements Initializable {
    @FXML
    private TextField file_path;
    @FXML
    private TextFlow text_flow;
    @FXML
    private NumberTextField edit_range;

    private ResourceBundle bundle;

    private AnalystService analyst;
    private File selectedFile;
    private List<String> strings;
    private Map<Integer, Color> colorMap;
    private Map<Character, Integer> ranges;

    @Override
    public void initialize(URL location, ResourceBundle bundle) {
        this.bundle = bundle;
    }

    @FXML
    private void openFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(bundle.getString("text_files"), "*.txt")
        );
        selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            file_path.setText(selectedFile.getAbsolutePath());
            readFile();
        }
    }

    private void readFile() {
        // Read file using NIO and stream of {@code String}
        try (Stream<String> stream = Files.lines(Paths.get(selectedFile.getAbsolutePath()))) {
            strings = stream.collect(Collectors.toList());

            List<Text> texts = strings.parallelStream()
                    .map(Text::new)
                    .collect(Collectors.toList());

            text_flow.getChildren().clear();
            text_flow.getChildren().addAll(texts);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(bundle.getString("read_error"));
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void analyse(ActionEvent event) {
        // Get number of range for letter frequency
        int numOfRange;
        String editText = edit_range.getText();
        if (editText.trim().length() > 0) {
            numOfRange = Integer.parseInt(edit_range.getText());
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(bundle.getString("input_err"));
            alert.setContentText(bundle.getString("input_mess"));
            alert.showAndWait();
            return;
        }

        if (strings == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(bundle.getString("information"));
            alert.setContentText(bundle.getString("mess_analyse"));
            alert.showAndWait();
            return;
        }

        analyst = new AnalystService(strings, numOfRange, bundle.getLocale());

        try {
            analyst.analyse();

            colorMap = generateColorMap(numOfRange);
            ranges = analyst.getRanges();

            // Add color to letter on the assumption of range
            List<Text> texts = strings.parallelStream()
                    .map(s -> s.chars())
                    .flatMap(intStream -> intStream.mapToObj(n -> (char) n))
                    .map(character -> {
                        String string = Character.toString(character);
                        Text text = new Text(string);
                        Character temp = Character.toUpperCase(character);
                        if (ranges.containsKey(temp)) {
                            text.setFill(colorMap.get(ranges.get(Character.toUpperCase(character))));
                        } else {
                            text.setFill(Color.BLACK);
                        }
                        return text;
                    })
                    .collect(Collectors.toList());

            text_flow.getChildren().clear();
            text_flow.getChildren().addAll(texts);
        } catch (IllegalArgumentException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Error of text language");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void showResult(ActionEvent event) throws Exception {
        if (ranges == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(bundle.getString("information"));
            alert.setContentText(bundle.getString("mess_noanalyse"));
            alert.showAndWait();
            return;
        }

        Stage stage = new Stage();
        BorderPane borderPane = new BorderPane();
        VBox vBox = new VBox();
        ScrollPane scrollPane = new ScrollPane();
        TextFlow textFlow = new TextFlow();
        HBox hBox = new HBox();
        Button button = new Button();

        button.setText(bundle.getString("create_histogram"));
        button.setOnAction(event1 -> {
            CategoryAxis xAxis = new CategoryAxis();
            NumberAxis yAxis = new NumberAxis();
            BarChart<String, Number> histogram = new BarChart<String, Number>(xAxis, yAxis);
            histogram.setTitle(bundle.getString("histogram_title"));
            xAxis.setLabel(bundle.getString("xAxis"));
            yAxis.setLabel(bundle.getString("yAxis"));

            // Count letters of ranges
            Map<Integer, Long> count = ranges.values().parallelStream()
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

            // Sort the letters by max frequency
            List<Character> characters = ranges.entrySet().parallelStream()
                    .sorted(Map.Entry.<Character, Integer>comparingByValue())
                    .map(entry -> entry.getKey())
                    .collect(Collectors.toList());

            // Create series for histogram that contain the number of range, letter and its frequency
            Map<Character, Long> characterCount = analyst.getCharacterCount();
            int start = 0;
            int stop = 0;
            List<XYChart.Series<String, Number>> seriesList = new ArrayList<XYChart.Series<String, Number>>();
            for (Map.Entry<Integer, Long> entry : count.entrySet()) {
                XYChart.Series<String, Number> series = new XYChart.Series<String, Number>();
                series.setName(Integer.toString(entry.getKey()));
                stop += entry.getValue().intValue();
                for (int i = start; i < stop; i++) {
                    Character character = characters.get(i);
                    series.getData().add(
                            new XYChart.Data<>(Character.toString(character), characterCount.get(character)));
                    start++;
                }
                seriesList.add(series);
            }

            ObservableList<XYChart.Series<String, Number>> list = FXCollections.observableArrayList(seriesList);
            histogram.setData(list);

            borderPane.setBottom(histogram);
        });

        // Make textFlow scrollable
        textFlow.getChildren().addListener((ListChangeListener<Node>) ((change) -> {
            textFlow.requestLayout();
            scrollPane.requestLayout();
            scrollPane.setVvalue(1.0F);
        }));

        scrollPane.setContent(textFlow);

        // Add frequency and color to letter on the assumption of range
        List<Text> texts = ranges.entrySet().parallelStream()
                .sorted(Map.Entry.<Character, Integer>comparingByValue())
                .map(entry -> {
                    String string = Character.toString(entry.getKey()) + " - " +
                            analyst.getCharacterCount().get(entry.getKey());
                    Text text = new Text(string + "\n");
                    text.setFill(colorMap.get(ranges.get(entry.getKey())));
                    return text;
                })
                .collect(Collectors.toList());

        textFlow.getChildren().addAll(texts);

        hBox.setHgrow(button, Priority.ALWAYS);
        button.setMaxWidth(Double.MAX_VALUE);
        hBox.getChildren().add(button);

        vBox.getChildren().addAll(scrollPane, button);

        borderPane.setCenter(vBox);

        stage.setTitle(bundle.getString("stage"));
        stage.setScene(new Scene(borderPane, 700, 500));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.show();
    }

    /**
     * Generate colors for range
     *
     * @param size size of range
     * @return Map<Number of range, Its color>
     */
    private Map<Integer, Color> generateColorMap(int size) {
        Map<Integer, Color> colorMap = new HashMap<>(size);
        Random random = new Random(System.currentTimeMillis());
        for (int i = 0; i < size; i++) {
            Color color = generateColor(random);
            while (color == Color.WHITE || color == Color.BLACK || color == Color.GRAY
                    || color == Color.LIGHTGRAY || color == Color.DARKGRAY || color == Color.TRANSPARENT) {
                color = generateColor(random);
            }
            colorMap.put(i, color);
        }
        return colorMap;
    }

    // Generate color using RGB
    private Color generateColor(Random random) {
        int red = random.nextInt(256);
        int green = random.nextInt(256);
        int blue = random.nextInt(256);

        return Color.rgb(red, green, blue);
    }

}