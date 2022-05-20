module pl.crypto.elgamalcrypto {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires org.apache.commons.io;

    opens pl.crypto to javafx.fxml;
    exports pl.crypto;

}