import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceDialog;
import javafx.stage.Stage;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * Application for calculating letter frequency using Stream API
 *
 * @author tkaczenko
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        List<String> languages = new ArrayList<>();
        languages.add("en");
        languages.add("ru");
        languages.add("uk");

        ChoiceDialog<String> dialog = new ChoiceDialog<>("en", languages);
        dialog.setTitle("Choose language...");
        dialog.setHeaderText("Choose language for analysis");
        dialog.setContentText("Choose your language");
        Optional<String> result = dialog.showAndWait();

        if (result.isPresent()) {
            ResourceBundle bundle = ResourceBundle.getBundle("resources.Resources",
                    new Locale(result.get()),
                    new ResourceBundle.Control() {
                        public List<String> getFormats(String baseName) {
                            if (baseName == null)
                                throw new NullPointerException();
                            return Arrays.asList("xml");
                        }

                        public ResourceBundle newBundle(String baseName,
                                                        Locale locale,
                                                        String format,
                                                        ClassLoader loader,
                                                        boolean reload)
                                throws IllegalAccessException,
                                InstantiationException,
                                IOException {
                            if (baseName == null || locale == null
                                    || format == null || loader == null)
                                throw new NullPointerException();
                            ResourceBundle bundle = null;
                            if (format.equals("xml")) {
                                String bundleName = toBundleName(baseName, locale);
                                String resourceName = toResourceName(bundleName, format);
                                InputStream stream = null;
                                if (reload) {
                                    URL url = loader.getResource(resourceName);
                                    if (url != null) {
                                        URLConnection connection = url.openConnection();
                                        if (connection != null) {
                                            connection.setUseCaches(false);
                                            stream = connection.getInputStream();
                                        }
                                    }
                                } else {
                                    stream = loader.getResourceAsStream(resourceName);
                                }
                                if (stream != null) {
                                    BufferedInputStream bis = new BufferedInputStream(stream);
                                    bundle = new XMLResourceBundle(bis);
                                    bis.close();
                                }
                            }
                            return bundle;
                        }
                    });
            Parent root = FXMLLoader.load(getClass().getResource("gui/analyser.fxml"), bundle);
            primaryStage.setTitle(bundle.getString("app_name"));
            primaryStage.setScene(new Scene(root));
            primaryStage.show();
        }
    }

    private static class XMLResourceBundle extends ResourceBundle {
        private Properties props;

        XMLResourceBundle(InputStream stream) throws IOException {
            props = new Properties();
            props.loadFromXML(stream);
        }

        protected Object handleGetObject(String key) {
            return props.getProperty(key);
        }

        public Enumeration<String> getKeys() {
            Set<String> handleKeys = props.stringPropertyNames();
            return Collections.enumeration(handleKeys);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
