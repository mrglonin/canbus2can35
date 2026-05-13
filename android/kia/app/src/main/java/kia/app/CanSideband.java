package kia.app;

import android.content.Context;

import java.util.Arrays;
import java.util.Locale;

final class CanSideband {
    private CanSideband() {
    }

    static Frame parse(byte[] frame) {
        if (frame == null || frame.length != 22) return null;
        if ((frame[4] & 0xff) != 0x70 || (frame[5] & 0xff) != 0x02) return null;
        int bus = frame[6] & 0xff;
        int flags = frame[7] & 0xff;
        int canId = (frame[8] & 0xff)
                | ((frame[9] & 0xff) << 8)
                | ((frame[10] & 0xff) << 16)
                | ((frame[11] & 0xff) << 24);
        int dlc = Math.min(frame[12] & 0xff, 8);
        byte[] data = Arrays.copyOfRange(frame, 13, 13 + dlc);
        return new Frame(bus, flags, canId, dlc, data);
    }

    static Frame parseRawCan(byte[] frame) {
        if (frame == null || frame.length < 23) return null;
        if ((frame[4] & 0xff) != 0x76) return null;
        int status = frame[5] & 0xff;
        if (status == 0) return null;
        int bus = frame[6] & 0xff;
        int flags = frame[7] & 0xff;
        int dlc = Math.min(frame[8] & 0xff, 8);
        if (dlc <= 0) return null;
        int canId = (frame[10] & 0xff)
                | ((frame[11] & 0xff) << 8)
                | ((frame[12] & 0xff) << 16)
                | ((frame[13] & 0xff) << 24);
        if (canId == 0) return null;
        byte[] data = Arrays.copyOfRange(frame, 14, 14 + dlc);
        return new Frame(bus, flags, status, canId, dlc, data);
    }

    static void apply(Context context, Frame frame) {
        if (frame == null) return;
        boolean handled = VehicleCanParser.handles(frame.canId);
        boolean debug = context != null && AppPrefs.debugCan(context);
        if (debug || SidebandDebugState.canRecording() || handled) {
            SidebandDebugState.can(context, frame);
        }
        if (context != null && handled && (AppPrefs.obdEnabled(context) || AppPrefs.blindSpotEnabled(context))) {
            VehicleCanParser.apply(context, frame);
        }
    }

    static byte[] canTxPayload(int bus, int canId, byte[] data) {
        return canTxPayload(bus, 0, canId, data);
    }

    static byte[] canTxPayload(int bus, int flags, int canId, byte[] data) {
        byte[] safe = data == null ? new byte[0] : Arrays.copyOf(data, Math.min(data.length, 8));
        byte[] payload = new byte[15];
        payload[0] = (byte) (bus == 1 ? 1 : 0);
        payload[1] = (byte) (flags & 0x03);
        payload[2] = (byte) (canId & 0xff);
        payload[3] = (byte) ((canId >> 8) & 0xff);
        payload[4] = (byte) ((canId >> 16) & 0xff);
        payload[5] = (byte) ((canId >> 24) & 0xff);
        payload[6] = (byte) safe.length;
        System.arraycopy(safe, 0, payload, 7, safe.length);
        return payload;
    }

    static final class Frame {
        final int bus;
        final int flags;
        final int valid;
        final int canId;
        final int dlc;
        final byte[] data;
        final long time;

        Frame(int bus, int flags, int canId, int dlc, byte[] data) {
            this(bus, flags, 1, canId, dlc, data);
        }

        Frame(int bus, int flags, int valid, int canId, int dlc, byte[] data) {
            this.bus = bus;
            this.flags = flags;
            this.valid = valid;
            this.canId = canId;
            this.dlc = dlc;
            this.data = data == null ? new byte[0] : data;
            this.time = System.currentTimeMillis();
        }

        String busName() {
            if (bus == 0) return "C-CAN/CAN1";
            if (bus == 1) return "M-CAN/CAN2";
            return "CAN?";
        }

        String flagText() {
            if (flags == 0) return "STD";
            StringBuilder sb = new StringBuilder();
            if ((flags & 0x01) != 0) sb.append("EXT");
            if ((flags & 0x02) != 0) {
                if (sb.length() > 0) sb.append('+');
                sb.append("RTR");
            }
            int extra = flags & ~0x03;
            if (extra != 0) {
                if (sb.length() > 0) sb.append('+');
                sb.append("flags=0x").append(Integer.toHexString(flags).toUpperCase(Locale.US));
            }
            return sb.length() == 0 ? "STD" : sb.toString();
        }

        String text() {
            return String.format(Locale.US, "status=%d bus=%d %s flags=%s id=0x%03X dlc=%d data=%s",
                    valid, bus, busName(), flagText(), canId, dlc, CanbusControl.hex(data));
        }
    }
}
