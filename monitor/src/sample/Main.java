package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class Main extends Application {
    private GridPane pane;
    private XYChart.Series gyroXSeries;
    private XYChart.Series gyroYSeries;
    private XYChart.Series gyroZSeries;

    @Override
    public void start(Stage stage) throws Exception
    {
        GridPane root = (GridPane)FXMLLoader.load(getClass().getResource("main.fxml"));
        Scene scene  = new Scene(root,1024,800);

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
