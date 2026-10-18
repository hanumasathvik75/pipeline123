package ai.ignition.android.obd;

/**
 * Transport mechanism confirmed for the physical ELM327 OBD adapter.
 */
public enum TransportType {
    CLASSIC_SPP_CONFIRMED,
    BLE_GATT_CONFIRMED,
    WIFI_ADAPTER,
    USB_ADAPTER,
    UNKNOWN_NEEDS_DOCUMENTATION
}
