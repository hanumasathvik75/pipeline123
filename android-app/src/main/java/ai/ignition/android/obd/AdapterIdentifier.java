package ai.ignition.android.obd;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.ParcelUuid;
import ai.ignition.android.util.RedactionUtil;

/**
 * Identifies whether an ELM327 adapter is Bluetooth Classic SPP or Bluetooth Low Energy (BLE GATT),
 * captures transport evidence, and instantiates the verified transport implementation.
 */
@SuppressLint("MissingPermission")
public class AdapterIdentifier {

    public static class IdentificationResult {
        public final TransportType transportType;
        public final String evidence;
        public final ObdTransport transport;

        public IdentificationResult(TransportType transportType, String evidence, ObdTransport transport) {
            this.transportType = transportType;
            this.evidence = evidence;
            this.transport = transport;
        }
    }

    /**
     * Inspects a BluetoothDevice to determine its physical transport and create the proper ObdTransport.
     */
    public static IdentificationResult identify(Context context, BluetoothDevice device) {
        if (device == null) {
            return new IdentificationResult(
                    TransportType.UNKNOWN_NEEDS_DOCUMENTATION,
                    "Device reference is null",
                    null
            );
        }

        int type = device.getType();
        boolean isBonded = device.getBondState() == BluetoothDevice.BOND_BONDED;
        StringBuilder evidence = new StringBuilder();

        evidence.append("Device Name: ").append(device.getName() != null ? device.getName() : "Unknown").append("\n");
        evidence.append("Device Address: ").append(RedactionUtil.redactMac(device.getAddress())).append("\n");
        evidence.append("Bond State: ").append(isBonded ? "BONDED (Paired in OS)" : "NOT BONDED").append("\n");
        evidence.append("Bluetooth Type: ");

        switch (type) {
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                evidence.append("DEVICE_TYPE_CLASSIC (1)\n");
                evidence.append("Transport Evidence: Classic RFCOMM SPP supported. Paired via system Bluetooth.\n");
                return new IdentificationResult(
                        TransportType.CLASSIC_SPP_CONFIRMED,
                        evidence.toString(),
                        new ClassicSppTransport(device)
                );

            case BluetoothDevice.DEVICE_TYPE_LE:
                evidence.append("DEVICE_TYPE_LE (2)\n");
                evidence.append("Transport Evidence: Low Energy GATT peripheral. Uses GATT serial characteristics.\n");
                return new IdentificationResult(
                        TransportType.BLE_GATT_CONFIRMED,
                        evidence.toString(),
                        new BleGattTransport(context, device)
                );

            case BluetoothDevice.DEVICE_TYPE_DUAL:
                evidence.append("DEVICE_TYPE_DUAL (3)\n");
                // Check if device advertised SPP UUID
                ParcelUuid[] uuids = device.getUuids();
                boolean hasSpp = false;
                if (uuids != null) {
                    for (ParcelUuid u : uuids) {
                        if (u != null && u.getUuid().equals(ClassicSppTransport.STANDARD_SPP_UUID)) {
                            hasSpp = true;
                            break;
                        }
                    }
                }
                if (isBonded || hasSpp) {
                    evidence.append("Transport Evidence: Dual-mode adapter with active Classic pairing/SPP profile.\n");
                    return new IdentificationResult(
                            TransportType.CLASSIC_SPP_CONFIRMED,
                            evidence.toString(),
                            new ClassicSppTransport(device)
                    );
                } else {
                    evidence.append("Transport Evidence: Dual-mode adapter selected over BLE GATT transport.\n");
                    return new IdentificationResult(
                            TransportType.BLE_GATT_CONFIRMED,
                            evidence.toString(),
                            new BleGattTransport(context, device)
                    );
                }

            default:
                evidence.append("DEVICE_TYPE_UNKNOWN (0)\n");
                if (isBonded) {
                    evidence.append("Transport Evidence: Paired device in system settings; adopting Classic SPP default.\n");
                    return new IdentificationResult(
                            TransportType.CLASSIC_SPP_CONFIRMED,
                            evidence.toString(),
                            new ClassicSppTransport(device)
                    );
                }
                evidence.append("Transport Evidence: Unknown transport without bonding info. Documentation needed.\n");
                return new IdentificationResult(
                        TransportType.UNKNOWN_NEEDS_DOCUMENTATION,
                        evidence.toString(),
                        null
                );
        }
    }
}
