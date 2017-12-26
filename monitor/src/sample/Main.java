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
        gyroXSeries = new XYChart.Series();
        gyroYSeries = new XYChart.Series();
        gyroZSeries = new XYChart.Series();
        gyroXSeries.setName("GX");
        gyroYSeries.setName("GY");
        gyroZSeries.setName("GZ");

        stage.setTitle("USLI Launch Monitor");
        final NumberAxis xAxis = new NumberAxis(-2048, 2048, 500);
        xAxis.setMinorTickCount(0);
        final NumberAxis yAxis = new NumberAxis(-2048, 2048, 500);
        yAxis.setMinorTickLength(yAxis.getTickLength());
        yAxis.setForceZeroInRange(false);

        final AreaChart<Number,Number> ac =
                new AreaChart<Number,Number>(xAxis,yAxis);
        ac.setTitle("Gyroscope Readings.");

        Scene scene  = new Scene(ac,800,600);
        ac.getData().addAll(gyroXSeries, gyroYSeries , gyroZSeries);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
