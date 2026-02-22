module systemtrayfx.samplerfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires systemtrayfx.core;

    opens systemtrayfx.samplerfx to javafx.fxml;
    exports systemtrayfx.samplerfx;
    exports systemtrayfx.samplerfx.controller;
    opens systemtrayfx.samplerfx.controller to javafx.fxml;
    exports systemtrayfx.samplerfx.model;
    opens systemtrayfx.samplerfx.model to javafx.fxml;
    exports systemtrayfx.samplerfx.enums;
    opens systemtrayfx.samplerfx.enums to javafx.fxml;
}