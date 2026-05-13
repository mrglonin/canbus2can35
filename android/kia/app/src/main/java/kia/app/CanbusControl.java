package kia.app;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

final class CanbusControl {
    static final String ACTION_STATE = "com.kia.navi.CANBUS_CONTROL_STATE";
    static final String ACTION_FRAME_RECEIVED = "com.kia.navi.CANBUS_FRAME_RECEIVED";

    private static final String LEGACY_AMP_RECEIVED = "kia.app.AMP_DATA_RECEIVED";
    private static final String LEGACY_TPMS_RECEIVED = "kia.app.TPMS_DATA_RECEIVED";
    private static final String LEGACY_SETTING_RECEIVED = "kia.app.SETTING_DATA_RECEIVED";
    private static final String LEGACY_CONFIRMATION = "kia.app.CONFIRMATION";

    private static String adapterUid = "не получен";
    private static String firmwareVersion = "не получена";
    private static String updateStatus = "ожидание";
    private static long lastFrameAt;
    private static int lastUartAvailable = -1;
    private static int lastUartDropped = -1;

    private CanbusControl() {
    }

    static synchronized Snapshot snapshot() {
        return new Snapshot(adapterUid, firmwareVersion, updateStatus, lastFrameAt);
    }

    static void requestAdapterInfo(Context context) {
        AppLog.line(context, "CAN: запрос настроек, UID и версии адаптера");
        send(context, frame(0x60, 0x00, 0x00));
        send(context, frame(0x56, 0x01));
        send(context, frame(0x56, 0x02));
    }

    static void requestAmpSettings(Context context) {
        AppLog.line(context, "AMP: запрос настроек усилителя");
        byte[] data = new byte[]{(byte) 0xBB, 0x41, (byte) 0xA1, 0x07, 0x30, 0x01, 0x00};
        send(context, checksum(data));
    }

    static void setAmpSettings(Context context, int volume, int balance, int fader,
                               int bass, int mid, int treble, int mode, boolean save) {
        byte[] data = new byte[15];
        data[0] = (byte) 0xBB;
        data[1] = 0x41;
        data[2] = (byte) 0xA1;
        data[3] = 15;
        data[4] = 0x30;
        data[5] = (byte) (save ? 0x00 : 0x02);
        data[6] = (byte) clamp(volume, 0, 40);
        data[7] = (byte) clamp(balance, 0, 20);
        data[8] = (byte) clamp(fader, 0, 20);
        data[9] = (byte) clamp(bass, 0, 20);
        data[10] = (byte) clamp(mid, 0, 20);
        data[11] = (byte) clamp(treble, 0, 20);
        data[12] = (byte) clamp(mode, 0, 32);
        data[13] = 0;
        send(context, checksum(data));
        AppLog.line(context, "AMP: " + (save ? "сохранение" : "настройка")
                + " vol=" + (data[6] & 0xff)
                + " bal=" + ((data[7] & 0xff) - 10)
                + " fad=" + ((data[8] & 0xff) - 10)
                + " bass=" + ((data[9] & 0xff) - 10)
                + " mid=" + ((data[10] & 0xff) - 10)
                + " treble=" + ((data[11] & 0xff) - 10)
                + " mode=" + (data[12] & 0xff));
    }

    static void setSasRatio(Context context, int ratio) {
        int safe = Math.max(10, Math.min(50, ratio));
        AppPrefs.setSasRatio(context, safe);
        send(context, frame(0x60, 0x01, safe));
        AppLog.line(context, "CAN: отправлен SAS Ratio " + safe);
    }

    static void setEngineTemp(Context context, boolean enabled) {
        AppPrefs.setEngineTempEnabled(context, enabled);
        send(context, frame(0x60, 0x02, enabled ? 1 : 0));
        AppLog.line(context, "CAN: температура двигателя " + (enabled ? "включена" : "выключена"));
    }

    static void sendUpdateStart(Context context) {
        setUpdateStatus(context, "запуск режима прошивки");
        send(context, frame(0x55, 0x01));
    }

    static void sendUpdateFinish(Context context) {
        setUpdateStatus(context, "завершение прошивки");
        send(context, frame(0x55, 0x00));
    }

    static void sendUpdateBlock(Context context, int offset, byte[] block) {
        byte[] frame = new byte[25];
        frame[0] = (byte) 0xBB;
        frame[1] = 0x41;
        frame[2] = (byte) 0xA1;
        frame[3] = 25;
        frame[4] = 0x55;
        frame[5] = (byte) ((offset >> 16) & 0xff);
        frame[6] = (byte) ((offset >> 8) & 0xff);
        frame[7] = (byte) (offset & 0xff);
        for (int i = 0; i < 16; i++) {
            frame[8 + i] = block != null && i < block.length ? block[i] : (byte) 0xff;
        }
        send(context, checksum(frame));
    }

    static void startCanStream(Context context) {
        sendQuiet(context, packet(0x70, new byte[]{0x01}));
        AppLog.line(context, "CAN sideband: stream 0x70 включён");
    }

    static void stopCanStream(Context context) {
        sendQuiet(context, packet(0x70, new byte[]{0x00}));
        AppLog.line(context, "CAN sideband: stream 0x70 выключен");
    }

    static void requestUartStatus(Context context) {
        SidebandDebugState.uartStatus(context, 0, 0);
    }

    static void readUart(Context context, int maxBytes) {
        SidebandDebugState.uartStatus(context, 0, 0);
    }

    static void sendUart(Context context, byte[] data) {
        byte[] payload = data == null ? new byte[0] : java.util.Arrays.copyOf(data, Math.min(data.length, 48));
        SidebandDebugState.uartTx(context, payload);
        AppLog.line(context, "UART sideband: отключён в raw CAN logger сборке");
    }

    static void sendMediaTrack(Context context, String title) {
        send(context, textPacket(0x22, title, 16));
    }

    static void sendMediaMetadata(Context context, String source, String artist, String title) {
        sendQuiet(context, mediaSourcePacket(1, source));
        sendQuiet(context, mediaSourcePacket(2, artist));
        send(context, textPacket(0x22, title, 16));
    }

    static void sendRawCan(Context context, int bus, int canId, byte[] data, boolean dryRun) {
        sendRawCan(context, bus, 0, canId, data, dryRun);
    }

    static void sendRawCan(Context context, int bus, int flags, int canId, byte[] data, boolean dryRun) {
        if (!validCanTx(bus, data)) {
            AppLog.line(context, "CAN TX: ошибка параметров bus=" + bus
                    + " len=" + (data == null ? 0 : data.length));
            return;
        }
        byte[] payload = CanSideband.canTxPayload(bus, flags, canId, data);
        AppLog.line(context, (dryRun ? "CAN TX dry-run: " : "CAN TX: ")
                + canBusText(bus) + " flags=" + flagsText(flags) + " id=0x" + Integer.toHexString(canId).toUpperCase(Locale.US)
                + " data=" + hex(data));
        if (!dryRun) send(context, packet(0x74, payload));
    }

    static void sendRawCanQuiet(Context context, int bus, int canId, byte[] data) {
        sendRawCanQuiet(context, bus, 0, canId, data);
    }

    static void sendRawCanQuiet(Context context, int bus, int flags, int canId, byte[] data) {
        if (!validCanTx(bus, data)) return;
        sendQuiet(context, packet(0x74, CanSideband.canTxPayload(bus, flags, canId, data)));
    }

    static void handleIncomingFrame(Context context, byte[] frame) {
        if (frame == null || frame.length < 6) return;
        synchronized (CanbusControl.class) {
            lastFrameAt = System.currentTimeMillis();
        }
        int id = frame[4] & 0xff;
        if (id != 0x76 && id != 0x71 && id != 0x72) {
            Intent frameIntent = new Intent(ACTION_FRAME_RECEIVED);
            frameIntent.setPackage(context.getPackageName());
            frameIntent.putExtra("frame", frame);
            context.sendBroadcast(frameIntent);
        }
        if (id == 0x70) {
            CanSideband.Frame canFrame = CanSideband.parse(frame);
            if (canFrame != null) CanSideband.apply(context, canFrame);
            return;
        }
        if (id == 0x76) {
            CanSideband.Frame canFrame = CanSideband.parseRawCan(frame);
            if (canFrame != null) CanSideband.apply(context, canFrame);
            return;
        }
        if (id == 0x71) {
            parseUartStatus(context, frame);
            return;
        }
        if (id == 0x72) {
            parseUartRead(context, frame);
            return;
        }
        if (id == 0x73) {
            AppLog.line(context, "UART TX ACK: " + ackText(frame) + " | " + hex(frame));
            return;
        }
        if (id == 0x74) {
            AppLog.line(context, "CAN TX ACK: " + ackText(frame) + " | " + hex(frame));
            return;
        }
        if (id == 0x30) {
            Intent intent = new Intent(LEGACY_AMP_RECEIVED);
            intent.putExtra("amp_data_bytes", frame);
            context.sendBroadcast(intent);
            return;
        }
        if (id == 0x51) {
            Intent intent = new Intent(LEGACY_TPMS_RECEIVED);
            intent.putExtra("tpms_data_bytes", frame);
            context.sendBroadcast(intent);
            applyCanTpms(context, frame);
            return;
        }
        if (id == 0x55) {
            Intent intent = new Intent(LEGACY_CONFIRMATION);
            intent.putExtra("confirmation", frame);
            context.sendBroadcast(intent);
            int code = frame.length > 5 ? frame[5] & 0xff : -1;
            setUpdateStatus(context, confirmationText(code));
            return;
        }
        if (id == 0x56) {
            Intent intent = new Intent(LEGACY_SETTING_RECEIVED);
            if ((frame[3] & 0xff) == 18) {
                intent.putExtra("uid_data_bytes", frame);
                updateUid(context, frame);
            } else if ((frame[3] & 0xff) == 10) {
                intent.putExtra("ver_data_bytes", frame);
                updateVersion(context, frame);
            }
            context.sendBroadcast(intent);
            return;
        }
        if (id == 0x60) {
            Intent intent = new Intent(LEGACY_SETTING_RECEIVED);
            intent.putExtra("set_data_confirmation", frame);
            context.sendBroadcast(intent);
            applySettingsFrame(context, frame);
        }
    }

    static String hex(byte[] data) {
        if (data == null) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            if (sb.length() > 0) sb.append(' ');
            int v = b & 0xff;
            if (v < 16) sb.append('0');
            sb.append(Integer.toHexString(v).toUpperCase(Locale.US));
        }
        return sb.toString();
    }

    private static void send(Context context, byte[] frame) {
        AppService.sendFrame(context, frame);
    }

    private static void sendQuiet(Context context, byte[] frame) {
        AppService.sendFrameQuiet(context, frame);
    }

    private static byte[] frame(int id, int command) {
        byte[] data = new byte[7];
        data[0] = (byte) 0xBB;
        data[1] = 0x41;
        data[2] = (byte) 0xA1;
        data[3] = 7;
        data[4] = (byte) id;
        data[5] = (byte) command;
        return checksum(data);
    }

    private static byte[] frame(int id, int command, int value) {
        byte[] data = new byte[8];
        data[0] = (byte) 0xBB;
        data[1] = 0x41;
        data[2] = (byte) 0xA1;
        data[3] = 8;
        data[4] = (byte) id;
        data[5] = (byte) command;
        data[6] = (byte) value;
        return checksum(data);
    }

    private static byte[] checksum(byte[] frame) {
        int len = frame[3] & 0xff;
        int sum = 0;
        for (int i = 0; i < len - 1 && i < frame.length; i++) {
            sum += frame[i] & 0xff;
        }
        if (len > 0 && len <= frame.length) frame[len - 1] = (byte) sum;
        return frame;
    }

    private static byte[] textPacket(int id, String value, int maxChars) {
        String text = value == null ? "" : value.trim();
        if (text.length() > maxChars) {
            text = text.substring(0, maxChars);
        }
        if (TextUtils.isEmpty(text)) {
            byte[] frame = new byte[]{(byte) 0xBB, 0x41, (byte) 0xA1, 0x08, (byte) id, 0, 0, 0};
            return checksum(frame);
        }
        byte[] payload = text.getBytes(StandardCharsets.UTF_16LE);
        int len = payload.length + 6;
        byte[] frame = new byte[len];
        frame[0] = (byte) 0xBB;
        frame[1] = 0x41;
        frame[2] = (byte) 0xA1;
        frame[3] = (byte) len;
        frame[4] = (byte) id;
        System.arraycopy(payload, 0, frame, 5, payload.length);
        return checksum(frame);
    }

    private static byte[] mediaSourcePacket(int type, String value) {
        String text = value == null ? "" : value.trim();
        if (text.length() > 16) {
            text = text.substring(0, 16);
        }
        if (TextUtils.isEmpty(text)) {
            byte[] frame = new byte[]{(byte) 0xBB, 0x41, (byte) 0xA1, 0x09, 0x21, (byte) type, 0, 0, 0};
            return checksum(frame);
        }
        byte[] payload = text.getBytes(StandardCharsets.UTF_16LE);
        int len = Math.min(payload.length, 32) + 7;
        byte[] frame = new byte[len];
        frame[0] = (byte) 0xBB;
        frame[1] = 0x41;
        frame[2] = (byte) 0xA1;
        frame[3] = (byte) len;
        frame[4] = 0x21;
        frame[5] = (byte) type;
        System.arraycopy(payload, 0, frame, 6, len - 7);
        return checksum(frame);
    }

    private static byte[] packet(int id, byte[] payload) {
        int size = payload == null ? 0 : payload.length;
        byte[] data = new byte[6 + size];
        data[0] = (byte) 0xBB;
        data[1] = 0x41;
        data[2] = (byte) 0xA1;
        data[3] = (byte) data.length;
        data[4] = (byte) id;
        if (size > 0) System.arraycopy(payload, 0, data, 5, size);
        return checksum(data);
    }

    private static void parseUartStatus(Context context, byte[] frame) {
        if (frame.length < 16) return;
        if (frame[5] != 'U' || frame[6] != 'A' || frame[7] != 'R' || frame[8] != 'T') return;
        int available = (frame[9] & 0xff) | ((frame[10] & 0xff) << 8);
        int dropped = (frame[11] & 0xff) | ((frame[12] & 0xff) << 8)
                | ((frame[13] & 0xff) << 16) | ((frame[14] & 0xff) << 24);
        SidebandDebugState.uartStatus(context, available, dropped);
        if (available != lastUartAvailable || dropped != lastUartDropped || available > 0) {
            lastUartAvailable = available;
            lastUartDropped = dropped;
            AppLog.line(context, "UART status: available=" + available + " dropped=" + dropped);
        }
        if (available > 0) {
            readUart(context, Math.min(available, 48));
        }
    }

    private static void parseUartRead(Context context, byte[] frame) {
        if (frame.length < 7) return;
        int count = Math.min(frame[5] & 0xff, Math.max(0, frame.length - 7));
        byte[] data = new byte[count];
        if (count > 0) System.arraycopy(frame, 6, data, 0, count);
        SidebandDebugState.uartRx(context, data);
    }

    private static void updateUid(Context context, byte[] frame) {
        if (frame.length < 17) return;
        StringBuilder sb = new StringBuilder();
        for (int i = 5; i <= 16; i++) {
            int v = frame[i] & 0xff;
            if (v < 16) sb.append('0');
            sb.append(Integer.toHexString(v).toUpperCase(Locale.US));
        }
        synchronized (CanbusControl.class) {
            adapterUid = sb.toString();
        }
        AppLog.line(context, "CAN: UID " + sb);
        broadcast(context);
    }

    private static void updateVersion(Context context, byte[] frame) {
        if (frame.length < 9) return;
        String family;
        int familyByte = frame[6] & 0xff;
        if (familyByte == 0x35) {
            family = "2CAN35";
        } else if (familyByte == 0x10) {
            family = "SIGMA10";
        } else {
            family = "CAN";
        }
        String prefix = (frame[5] & 0xff) == 0xAA ? "BASE " : "";
        String version = prefix + family + " " + byteHex(frame[7]) + "." + byteHex(frame[8]);
        synchronized (CanbusControl.class) {
            firmwareVersion = version;
        }
        AppLog.line(context, "CAN: версия " + version);
        broadcast(context);
    }

    private static void applySettingsFrame(Context context, byte[] frame) {
        int len = frame[3] & 0xff;
        int mode = frame.length > 5 ? frame[5] & 0xff : -1;
        if (mode == 0 && len == 9 && frame.length > 7) {
            int ratio = frame[6] & 0xff;
            boolean engineTemp = frame[7] != 0;
            AppPrefs.setSasRatio(context, ratio);
            AppPrefs.setEngineTempEnabled(context, engineTemp);
            AppLog.line(context, "CAN: настройки получены SAS=" + ratio + " темп=" + (engineTemp ? "да" : "нет"));
        } else if (mode == 1 && len == 8 && frame.length > 6) {
            int ratio = frame[6] & 0xff;
            AppPrefs.setSasRatio(context, ratio);
            AppLog.line(context, "CAN: SAS подтверждён " + ratio);
        } else if (mode == 2 && len == 8 && frame.length > 6) {
            boolean engineTemp = frame[6] != 0;
            AppPrefs.setEngineTempEnabled(context, engineTemp);
            AppLog.line(context, "CAN: температура двигателя подтверждена " + (engineTemp ? "да" : "нет"));
        }
        broadcast(context);
    }

    private static void applyCanTpms(Context context, byte[] frame) {
        if (context != null && !AppPrefs.tpmsEnabled(context)) return;
        if (frame.length < 13) return;
        TpmsState.status(context, "TPMS: данные получены от Kia Canbus", true);
        updateCanTire(context, 0, frame[6], frame[10]);
        updateCanTire(context, 1, frame[5], frame[9]);
        updateCanTire(context, 2, frame[7], frame[11]);
        updateCanTire(context, 3, frame[8], frame[12]);
    }

    private static void updateCanTire(Context context, int index, byte pressureRaw, byte tempRaw) {
        int p = pressureRaw & 0xff;
        if (p == 0 || p == 0xff) return;
        float pressure = (float) (p * 1.378952d / 100d);
        int temp = (tempRaw & 0xff) - 50;
        TpmsState.tire(context, index, pressure, temp, 0, false);
    }

    private static void setUpdateStatus(Context context, String value) {
        synchronized (CanbusControl.class) {
            updateStatus = value;
        }
        AppLog.line(context, "CAN прошивка: " + value);
        broadcast(context);
    }

    private static String confirmationText(int code) {
        if (code == 1) return "адаптер вошёл в режим прошивки";
        if (code == 2) return "блок прошивки принят";
        if (code == 0) return "адаптер отменил прошивку";
        return "ответ адаптера " + code;
    }

    private static boolean validCanTx(int bus, byte[] data) {
        int len = data == null ? 0 : data.length;
        return (bus == 0 || bus == 1) && len <= 8;
    }

    private static String canBusText(int bus) {
        if (bus == 0) return "bus=0 C-CAN/CAN1";
        if (bus == 1) return "bus=1 M-CAN/CAN2";
        return "bus=" + bus;
    }

    private static String flagsText(int flags) {
        int clean = flags & 0x03;
        if (clean == 0) return "STD";
        if (clean == 1) return "EXT";
        if (clean == 2) return "RTR";
        return "EXT+RTR";
    }

    private static String ackText(byte[] frame) {
        int code = frame != null && frame.length > 5 ? frame[5] & 0xff : -1;
        if (code == 0x00) return "00 кадр поставлен в mailbox";
        if (code == 0x01) return "01 mailbox занят";
        if (code == 0x02) return "02 неверный bus";
        if (code == 0xff) return "FF неверная длина";
        if (code < 0) return "нет payload";
        return "код " + byteHex((byte) code);
    }

    private static String byteHex(byte b) {
        int v = b & 0xff;
        return (v < 16 ? "0" : "") + Integer.toHexString(v).toUpperCase(Locale.US);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void broadcast(Context context) {
        if (context == null) return;
        Intent intent = new Intent(ACTION_STATE);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

    static final class Snapshot {
        final String adapterUid;
        final String firmwareVersion;
        final String updateStatus;
        final long lastFrameAt;

        Snapshot(String adapterUid, String firmwareVersion, String updateStatus, long lastFrameAt) {
            this.adapterUid = adapterUid;
            this.firmwareVersion = firmwareVersion;
            this.updateStatus = updateStatus;
            this.lastFrameAt = lastFrameAt;
        }
    }
}
