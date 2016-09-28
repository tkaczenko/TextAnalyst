package analyser;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
import javafx.scene.layout.AnchorPane;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        colorMap = generateColorMap((int) analyst.getDelta());
        ranges = analyst.getRanges();

        List<Text> texts = strings.parallelStream()
                .map(s -> s.chars())
                .flatMap(intStream -> intStream.mapToObj(n -> (char) n))
                .map(character -> {
                    String string = Character.toString(character);
                    Text text = new Text(string);
                    text.setFill(colorMap.get(ranges.get(Character.toUpperCase(character))));
                    return text;
                })
                .collect(Collectors.toList());

        text_flow.getChildren().clear();
        text_flow.getChildren().addAll(texts);
    }

    @FXML
    private void showResult(ActionEvent event) throws Exception {
        Stage stage = new Stage();
        VBox anchorPane = new VBox();
        ScrollPane scrollPane = new ScrollPane();
        TextFlow textFlow = new TextFlow();
        Button button = new Button();
        button.setText("Побудувати гістограму");
        button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Stage stage1 = new Stage();
                AnchorPane anchorPane1 = new AnchorPane();
                CategoryAxis xAxis = new CategoryAxis();
                NumberAxis yAxis = new NumberAxis();
                BarChart<String, Number> histogram = new BarChart<String, Number>(xAxis, yAxis);

                histogram.setTitle("Частотный аналіз літер тексту");
                xAxis.setLabel("Діапазон");
                yAxis.setLabel("Частота");

                int size = analyst.getNumOfRange();
                Map<Integer, Long> count = analyst.getRanges().values().stream()
                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

                System.out.println("RANGES");
                ranges.entrySet().stream().forEach(System.out::println);

                System.out.println("COUNT");
                count.entrySet().stream().forEach(System.out::println);

                List<Character> characters = ranges.entrySet().stream()
                        .sorted(Map.Entry.<Character, Integer>comparingByValue())
                        .map(entry -> entry.getKey())
                        .collect(Collectors.toList());
                System.out.println("CHARACTERS");
                characters.stream().forEach(System.out::println);

                List<XYChart.Series<String, Number>> seriesList = count.entrySet().stream()
                        .map(integerLongEntry -> {
                            XYChart.Series<String, Number> series = new XYChart.Series<String, Number>();
                            series.setName(Integer.toString(integerLongEntry.getKey()));
                            int start = integerLongEntry.getKey();
                            int delta = (start > 0) ? count.get(start - 1).intValue() : 0;
                            for (int i = start * delta; i < delta + integerLongEntry.getValue(); i++) {
                                Character temp = characters.get(i);
                                series.getData().add(new XYChart.Data<>(Character.toString(temp), analyst.getCharacterCount().get(temp)));
                            }
                            return series;
                        })
                        .collect(Collectors.toList());

                ObservableList<XYChart.Series<String, Number>> list = FXCollections.observableArrayList(seriesList);

                histogram.setData(list);

                anchorPane1.getChildren().add(histogram);

                stage1.setTitle("Гістогргама");
                stage1.setScene(new Scene(anchorPane1, 600, 400));
                stage1.show();
            }
        });

        textFlow.getChildren().addListener((ListChangeListener<Node>) ((change) -> {
            textFlow.requestLayout();
            scrollPane.requestLayout();
            scrollPane.setVvalue(1.0F);
        }));
        scrollPane.setContent(textFlow);

        List<Text> texts = analyst.getRanges().entrySet().stream()
                .sorted(Map.Entry.<Character, Integer>comparingByValue().reversed())
                .map(entry -> {
                    String string = Character.toString(entry.getKey()) + " - " +
                            analyst.getCharacterCount().get(entry.getKey());
                    Text text = new Text(string + "\n");
                    text.setFill(colorMap.get(analyst.getRanges().get(entry.getKey())));
                    return text;
                })
                .collect(Collectors.toList());

        textFlow.getChildren().addAll(texts);

        anchorPane.getChildren().addAll(scrollPane, button);

        stage.setTitle("Результати частотного аналізу");
        stage.setScene(new Scene(anchorPane, 600, 400));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.show();
    }

    private Map<Integer, Color> generateColorMap(int size) {
        Map<Integer, Color> colorMap = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            Color color = generateColor();
            while (color == Color.WHITE || color == Color.BLACK || color == Color.GRAY || color == Color.LIGHTGRAY) {
                color = generateColor();
            }
            colorMap.put(i, color);
        }
        return colorMap;
    }

    private Color generateColor() {
        return Color.color(Math.random(), Math.random(), Math.random());
    }

}