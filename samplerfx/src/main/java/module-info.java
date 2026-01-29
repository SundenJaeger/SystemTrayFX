module com.rentoki {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.systemtray.core;

    opens com.systemtray.samplerfx to javafx.fxml;
    exports com.systemtray.samplerfx;
}
