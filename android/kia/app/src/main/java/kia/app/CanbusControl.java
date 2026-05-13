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
    private static String v20Status = "не проверено";
    private static String v20Api = "—";
    private static String v20Capabilities = "—";
    private static String updateStatus = "ожидание";
    private static long lastFrameAt;
    private static long lastV20At;
    private static int lastUartAvailable = -1;
    private static int lastUartDropped = -1;
    private static int mediaSecond;

    private CanbusControl() {
    }

    static synchronized Snapshot snapshot() {
        return new Snapshot(adapterUid, firmwareVersion, v20Status, v20Api, v20Capabilities,
                updateStatus, lastFrameAt, lastV20At);
    }

    static void requestAdapterInfo(Context context) {
        AppLog.line(context, "CAN: запрос настроек, UID и версии адаптера");
        send(context, frame(0x60, 0x00, 0x00));
        send(context, frame(0x56, 0x01));
        send(context, frame(0x56, 0x02));
        requestV20Status(context);
    }

    static void requestV20Status(Context context) {
        AppLog.line(context, "V21: запрос health/capabilities 0x79");
        send(context, packet(0x79, null));
    }

    static void requestVehicleSnapshotQuiet(Context context) {
        sendQuiet(context, packet(0x77, null));
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
        AppLog.line(context, "UART sideband: отключён в CAN sideband сборке");
    }

    static void sendMediaTrack(Context context, String title) {
        send(context, mediaSourceStatusPacket(MediaSource.USB_MUSIC));
        send(context, textPacket(0x22, title, 16));
    }

    static void sendMediaMetadata(Context context, String source, String artist, String title) {
        MediaSource mediaSource = mediaSource(source);
        send(context, mediaSourceStatusPacket(mediaSource));
        switch (mediaSource) {
            case BLUETOOTH_AUDIO:
                send(context, textPacketWithSubtype(0x20, 0x1F, artist, 16));
                send(context, textPacket(0x22, title, 16));
                break;
            case FM_RADIO:
                send(context, textPacket(0x21, firstNonEmpty(title, sourceLabel(mediaSource, source)), 16));
                break;
            case AM_RADIO:
                send(context, textPacket(0x20, firstNonEmpty(title, sourceLabel(mediaSource, source)), 16));
                break;
            case BT_PHONE:
                AppLog.line(context, "Мультимедиа: BT phone source без text-поля");
                break;
            case USB_MUSIC:
            case CLOUD_MUSIC:
            case GENERIC_MUSIC:
            default:
                send(context, textPacket(0x22, firstNonEmpty(title, sourceLabel(mediaSource, source)), 16));
                break;
        }
    }

    static void sendNavigationSourceQuiet(Context context) {
        send(context, packet(0x7A, raiseFrame(new byte[]{0x06, 0x09, 0x06, 0x00, 0x00})));
    }

    static void sendCompassStep(Context context, int uiStep) {
        send(context, compassPacket(uiStep));
    }

    static void sendCompassStepQuiet(Context context, int uiStep) {
        sendQuiet(context, compassPacket(uiStep));
    }

    static void sendCompassHeadingQuiet(Context context, float headingDegrees) {
        int ui = Math.round(normalizeHeading(headingDegrees) / 30f) * 3;
        sendCompassStepQuiet(context, ui);
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
        if (!dryRun) send(context, packet(0x78, payload));
    }

    static void sendRawCanQuiet(Context context, int bus, int canId, byte[] data) {
        sendRawCanQuiet(context, bus, 0, canId, data);
    }

    static void sendRawCanQuiet(Context context, int bus, int flags, int canId, byte[] data) {
        if (!validCanTx(bus, data)) return;
        sendQuiet(context, packet(0x78, CanSideband.canTxPayload(bus, flags, canId, data)));
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
        if (id == 0x77) {
            parseVehicleSnapshot(context, frame);
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
            AppLog.line(context, "USB ACK UART TX: " + ackText(frame) + " | " + hex(frame));
            return;
        }
        if (id == 0x78) {
            AppLog.line(context, "USB ACK raw CAN TX: " + ackText(frame) + " | " + hex(frame));
            return;
        }
        if (id == 0x74) {
            AppLog.line(context, "USB ACK legacy raw TX 0x74: " + ackText(frame) + " | " + hex(frame));
            return;
        }
        if (id == 0x79) {
            updateV20Status(context, frame);
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
        QaState.frame(context, frame);
        AppService.sendFrame(context, frame);
    }

    private static void sendQuiet(Context context, byte[] frame) {
        QaState.frame(context, frame);
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

    private static byte[] textPacketWithSubtype(int id, int subtype, String value, int maxChars) {
        String text = value == null ? "" : value.trim();
        if (text.length() > maxChars) {
            text = text.substring(0, maxChars);
        }
        if (TextUtils.isEmpty(text)) {
            byte[] frame = new byte[]{(byte) 0xBB, 0x41, (byte) 0xA1, 0x09, (byte) id, (byte) subtype, 0, 0, 0};
            return checksum(frame);
        }
        byte[] payload = text.getBytes(StandardCharsets.UTF_16LE);
        int len = Math.min(payload.length, maxChars * 2) + 7;
        byte[] frame = new byte[len];
        frame[0] = (byte) 0xBB;
        frame[1] = 0x41;
        frame[2] = (byte) 0xA1;
        frame[3] = (byte) len;
        frame[4] = (byte) id;
        frame[5] = (byte) subtype;
        System.arraycopy(payload, 0, frame, 6, len - 7);
        return checksum(frame);
    }

    private static byte[] mediaSourceStatusPacket(MediaSource source) {
        return packet(0x7A, raiseSourceStatus(source));
    }

    private static byte[] raiseSourceStatus(MediaSource source) {
        if (source == MediaSource.BLUETOOTH_AUDIO) {
            return raiseFrame(new byte[]{0x06, 0x09, 0x0B, 0x04, 0x00});
        }
        if (source == MediaSource.FM_RADIO) {
            return raiseFrame(new byte[]{0x08, 0x09, 0x02, 0x00, 0x65, 0x46, 0x00});
        }
        if (source == MediaSource.AM_RADIO) {
            return raiseFrame(new byte[]{0x06, 0x09, 0x09, 0x00, 0x00});
        }
        if (source == MediaSource.BT_PHONE) {
            return raiseFrame(new byte[]{0x06, 0x09, 0x07, 0x01, 0x00});
        }
        int sec;
        synchronized (CanbusControl.class) {
            mediaSecond = (mediaSecond + 1) % 60;
            sec = mediaSecond;
        }
        return raiseFrame(new byte[]{0x0A, 0x09, 0x16, 0x00, 0x01, 0x00, 0x00, (byte) sec});
    }

    private static byte[] raiseFrame(byte[] body) {
        byte[] frame = new byte[body.length + 2];
        frame[0] = (byte) 0xFD;
        System.arraycopy(body, 0, frame, 1, body.length);
        int sum = 0;
        for (int i = 1; i < frame.length - 1; i++) sum += frame[i] & 0xff;
        frame[frame.length - 1] = (byte) sum;
        return frame;
    }

    private static MediaSource mediaSource(String source) {
        String text = source == null ? "" : source.toLowerCase(Locale.US);
        if (text.contains("phone") || text.contains("телефон")) return MediaSource.BT_PHONE;
        if (text.contains("bluetooth") || text.contains("bt")) return MediaSource.BLUETOOTH_AUDIO;
        if (text.contains("интернет") || text.contains("network")) return MediaSource.CLOUD_MUSIC;
        if (text.contains("fm") || text.contains("радио") || text.contains("radio")) return MediaSource.FM_RADIO;
        if (text.contains("am ")) return MediaSource.AM_RADIO;
        if (text.contains("яндекс") || text.contains("yandex") || text.contains("spotify")
                || text.contains("youtube") || text.contains("cloud") || text.contains("teyes music")) {
            return MediaSource.CLOUD_MUSIC;
        }
        if (text.contains("usb") || text.contains("local")) return MediaSource.USB_MUSIC;
        return MediaSource.GENERIC_MUSIC;
    }

    private static String sourceLabel(MediaSource mediaSource, String source) {
        if (!TextUtils.isEmpty(source)) return source;
        switch (mediaSource) {
            case BLUETOOTH_AUDIO:
                return "Bluetooth";
            case FM_RADIO:
                return "FM";
            case AM_RADIO:
                return "AM";
            case BT_PHONE:
                return "BT phone";
            case CLOUD_MUSIC:
                return "Cloud music";
            case USB_MUSIC:
            case GENERIC_MUSIC:
            default:
                return "USB";
        }
    }

    private enum MediaSource {
        USB_MUSIC,
        BLUETOOTH_AUDIO,
        FM_RADIO,
        AM_RADIO,
        BT_PHONE,
        CLOUD_MUSIC,
        GENERIC_MUSIC
    }

    private static byte[] compassPacket(int uiStep) {
        int ui = nearestCompassUiStep(uiStep);
        int sent = (36 - ui) % 36;
        byte[] frame = new byte[]{(byte) 0xBB, 0x41, (byte) 0xA1, 0x0E, 0x45, 0x08, 0x00, 0x00,
                (byte) sent, 0x00, 0x78, 0x00, (byte) 0xA0, 0x00};
        return checksum(frame);
    }

    private static int nearestCompassUiStep(int value) {
        int out = ((value % 36) + 36) % 36;
        out = Math.round(out / 3f) * 3;
        return out == 36 ? 0 : out;
    }

    private static float normalizeHeading(float value) {
        float out = value % 360f;
        if (out < 0f) out += 360f;
        return out;
    }

    private static String firstNonEmpty(String first, String fallback) {
        return TextUtils.isEmpty(first) ? fallback : first;
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

    private static void parseVehicleSnapshot(Context context, byte[] frame) {
        int frameLen = frame == null || frame.length < 6 ? 0 : Math.min(frame.length, frame[3] & 0xff);
        int payloadLen = frameLen >= 6 ? frameLen - 6 : 0;
        if (payloadLen < 30) return;
        int base = 5;
        int status = u8(frame, base);
        if (status == 0) return;
        int known = u8(frame, base + 1) | (u8(frame, base + 2) << 8) | (u8(frame, base + 3) << 16);
        if ((known & 0x001) != 0) ObdState.speed(context, u16(frame, base + 8));
        if ((known & 0x002) != 0) ObdState.rpm(context, u16(frame, base + 10));
        if ((known & 0x004) != 0) ObdState.coolant(context, s16(frame, base + 12));
        if ((known & 0x008) != 0) {
            int voltage = u16(frame, base + 14);
            if (voltage > 0) ObdState.voltage(context, voltage >= 1000 ? voltage / 1000f : voltage / 10f);
        }
        if ((known & 0x010) != 0) ObdState.throttle(context, u8(frame, base + 16));
        if ((known & 0x040) != 0) BlindSpotState.reverse(context, u8(frame, base + 18) == 2);
        if ((known & 0x400) != 0) ObdState.intake(context, s16(frame, base + 20));
        if (payloadLen >= 45 && (u8(frame, base + 30) & 0x01) != 0) {
            int bus = u8(frame, base + 31);
            int dlc = Math.min(u8(frame, base + 32), 8);
            int canId = le32(frame, base + 33);
            if (dlc > 0 && canId != 0) {
                byte[] data = new byte[dlc];
                System.arraycopy(frame, base + 37, data, 0, dlc);
                CanSideband.apply(context, new CanSideband.Frame(bus, 0, canId, dlc, data));
            }
        }
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

    private static void updateV20Status(Context context, byte[] frame) {
        int len = frame == null || frame.length < 6 ? 0 : frame[3] & 0xff;
        int status = frame != null && frame.length > 5 ? frame[5] & 0xff : -1;
        int apiMajor = frame != null && frame.length > 6 ? frame[6] & 0xff : -1;
        int apiMinor = frame != null && frame.length > 7 ? frame[7] & 0xff : -1;
        int rawTx = frame != null && frame.length > 8 ? frame[8] & 0xff : -1;
        int caps = frame != null && frame.length > 9 ? frame[9] & 0xff : -1;
        String tag = ascii(frame, 10, Math.max(0, len - 11));
        String statusText = status == 0x20 ? "V20 online"
                : status == 0x21 ? "V21 online"
                : "ответ 0x" + byteHex((byte) status);
        String apiText = apiMajor >= 0 ? "API " + apiMajor + "." + Math.max(0, apiMinor) : "API ?";
        String capsText = "rawTX=" + (rawTx == 0 ? "нет" : "да")
                + " caps=0x" + byteHex((byte) Math.max(0, caps))
                + (TextUtils.isEmpty(tag) ? "" : " tag=" + tag);
        synchronized (CanbusControl.class) {
            v20Status = statusText;
            v20Api = apiText;
            v20Capabilities = capsText;
            lastV20At = System.currentTimeMillis();
        }
        AppLog.line(context, "V21: " + statusText + " " + apiText + " " + capsText);
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
        return bus == 1 && len <= 8;
    }

    private static String canBusText(int bus) {
        if (bus == 0) return "bus=0 C-CAN/CAN1 blocked";
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

    private static String ascii(byte[] data, int offset, int maxLen) {
        if (data == null || offset >= data.length || maxLen <= 0) return "";
        StringBuilder sb = new StringBuilder();
        int end = Math.min(data.length - 1, offset + maxLen);
        for (int i = offset; i < end; i++) {
            int v = data[i] & 0xff;
            if (v == 0) break;
            if (v >= 32 && v <= 126) sb.append((char) v);
        }
        return sb.toString();
    }

    private static int u8(byte[] data, int offset) {
        return data == null || offset < 0 || offset >= data.length ? 0 : data[offset] & 0xff;
    }

    private static int u16(byte[] data, int offset) {
        return u8(data, offset) | (u8(data, offset + 1) << 8);
    }

    private static int s16(byte[] data, int offset) {
        int value = u16(data, offset);
        return value >= 0x8000 ? value - 0x10000 : value;
    }

    private static int le32(byte[] data, int offset) {
        return u8(data, offset)
                | (u8(data, offset + 1) << 8)
                | (u8(data, offset + 2) << 16)
                | (u8(data, offset + 3) << 24);
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
        final String v20Status;
        final String v20Api;
        final String v20Capabilities;
        final String updateStatus;
        final long lastFrameAt;
        final long lastV20At;

        Snapshot(String adapterUid, String firmwareVersion, String v20Status, String v20Api,
                 String v20Capabilities, String updateStatus, long lastFrameAt, long lastV20At) {
            this.adapterUid = adapterUid;
            this.firmwareVersion = firmwareVersion;
            this.v20Status = v20Status;
            this.v20Api = v20Api;
            this.v20Capabilities = v20Capabilities;
            this.updateStatus = updateStatus;
            this.lastFrameAt = lastFrameAt;
            this.lastV20At = lastV20At;
        }
    }
}
