package in.co.xyon.application.android.module.deviceconfig.domain.lib;


/**
 * Interface for QR code scanning callbacks.
 */
public interface QRCodeScanListenerMod {

    /**
     * Called when QR code is scanned so that app can display loading.
     */
    void qrCodeScanned();

    /**
     * Called when received device from QR code is available in scanning.
     *
     * @param espDevice
     */
    void deviceDetected(ESPDeviceMod espDevice);

    /**
     * Failed to scan QR code or device not found in scanning.
     *
     * @param e Exception
     */
    void onFailure(Exception e);

    /**
     * Called when QR code data has different format.
     *
     * @param e    Exception
     * @param data QR code data string.
     */
    void onFailure(Exception e, String data);
}

