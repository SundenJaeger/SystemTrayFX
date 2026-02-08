module com.rentoki {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.systemtray.core;

    opens com.systemtray.samplerfx to javafx.fxml;
    exports com.systemtray.samplerfx;
    exports com.systemtray.samplerfx.controller;
    opens com.systemtray.samplerfx.controller to javafx.fxml;
    exports com.systemtray.samplerfx.model;
    opens com.systemtray.samplerfx.model to javafx.fxml;
    exports com.systemtray.samplerfx.enums;
    opens com.systemtray.samplerfx.enums to javafx.fxml;
}
