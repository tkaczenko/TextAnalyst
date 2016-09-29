package analyser;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.AnalystService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Controller {
    @FXML
    private TextField file_path;
    @FXML
    private TextFlow text_flow;
    @FXML
    private TextField edit_range;

    private AnalystService analyst = new AnalystService();
    private File selectedFile;
    private List<String> strings;
    private Map<Integer, Color> colorMap;
    private Map<Character, Integer> ranges;

    @FXML
    public void initialize() {
        ScrollPane scrollPane = new ScrollPane();
        text_flow.getChildren().addListener((ListChangeListener<Node>) ((change) -> {
            text_flow.requestLayout();
            scrollPane.requestLayout();
            scrollPane.setVvalue(1.0F);
        }));
        scrollPane.setContent(text_flow);
    }

    @FXML
    private void openFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text files", "*.txt")
        );
        selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            file_path.setText(selectedFile.getAbsolutePath());
            readFile();
        }
    }

    private void readFile() {
        try (Stream<String> stream = Files.lines(Paths.get(selectedFile.getAbsolutePath()))) {
            strings = stream.collect(Collectors.toList());

            List<Text> texts = strings.parallelStream()
                    .map(Text::new)
                    .collect(Collectors.toList());

            text_flow.getChildren().addAll(texts);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void analyse(ActionEvent event) {
        int numOfRange = Integer.parseInt(edit_range.getText());

        analyst.setStrings(strings);
        analyst.setNumOfRange(numOfRange);

        analyst.analyse();
        colorMap = generateColorMap(numOfRange);
        ranges = analyst.getRanges();

        System.out.println(Character.toString('.'));

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
    }

    @FXML
    private void showResult(ActionEvent event) throws Exception {
        Stage stage = new Stage();
        BorderPane borderPane = new BorderPane();
        ScrollPane scrollPane = new ScrollPane();
        TextFlow textFlow = new TextFlow();
        Button button = new Button();
        button.setText("Побудувати гістограму");
        button.setOnAction(event1 -> {
            Stage histogramStage = new Stage();
            BorderPane borderPaneHistogram = new BorderPane();

            CategoryAxis xAxis = new CategoryAxis();
            NumberAxis yAxis = new NumberAxis();
            BarChart<String, Number> histogram = new BarChart<String, Number>(xAxis, yAxis);
            histogram.setTitle("Частотный аналіз літер тексту");
            xAxis.setLabel("Діапазон");
            yAxis.setLabel("Частота");

            Map<Integer, Long> count = ranges.values().stream()
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

            List<Character> characters = ranges.entrySet().stream()
                    .sorted(Map.Entry.<Character, Integer>comparingByValue())
                    .map(entry -> entry.getKey())
                    .collect(Collectors.toList());

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

            borderPaneHistogram.setCenter(histogram);

            histogramStage.setTitle("Гістогргама");
            histogramStage.setScene(new Scene(borderPaneHistogram));
            histogramStage.show();
        });

        textFlow.getChildren().addListener((ListChangeListener<Node>) ((change) -> {
            textFlow.requestLayout();
            scrollPane.requestLayout();
            scrollPane.setVvalue(1.0F);
        }));

        scrollPane.setContent(textFlow);

        List<Text> texts = ranges.entrySet().stream()
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

        borderPane.setCenter(scrollPane);
        borderPane.setBottom(button);

        stage.setTitle("Частоти");
        stage.setScene(new Scene(borderPane));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.show();
    }

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

    private Color generateColor(Random random) {
        int red = random.nextInt(256);
        int green = random.nextInt(256);
        int blue = random.nextInt(256);

        return Color.rgb(red, green, blue);
    }

}