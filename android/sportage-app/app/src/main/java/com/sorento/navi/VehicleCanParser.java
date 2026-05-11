package com.sorento.navi;

import android.content.Context;

final class VehicleCanParser {
    private static String lastBody = "";
    private static String lastRearDoors = "";
    private static String lastReverse = "";
    private static String lastParking = "";
    private static String lastClimate = "";
    private static String lastStatus = "";
    private static long lastStatusAt;

    private VehicleCanParser() {
    }

    static void apply(Context context, CanSideband.Frame frame) {
        if (context == null || frame == null || frame.data == null) return;
        byte[] d = frame.data;
        int id = frame.canId;

        if (id == 0x316 && d.length >= 8) {
            ObdState.speed(context, d[6] & 0xff);
            int rpmRaw = (d[2] & 0xff) | ((d[3] & 0xff) << 8);
            ObdState.rpm(context, rpmRaw / 4);
            status(context, "OBD: raw CAN 0x76: скорость/обороты");
            return;
        }
        if (id == 0x329 && d.length >= 2) {
            int temp = Math.round(((d[1] & 0xff) - 0x40) * 0.75f);
            ObdState.coolant(context, temp);
            status(context, "OBD: raw CAN 0x76: температура двигателя");
            return;
        }
        if (id == 0x044 && d.length >= 4) {
            int outside = Math.round(((d[3] & 0xff) - 0x52) / 2f);
            ObdState.intake(context, outside);
            status(context, "OBD: raw CAN 0x76: наружная температура");
            return;
        }
        if (id == 0x545 && d.length >= 4) {
            ObdState.voltage(context, (d[3] & 0xff) / 10f);
            status(context, "OBD: raw CAN 0x76: напряжение АКБ");
            return;
        }
        if (id == 0x541 && d.length >= 8) {
            boolean ignition = (d[0] & 0x03) != 0;
            String doors = "";
            if ((d[1] & 0x01) != 0) doors += "ЛП ";
            if ((d[4] & 0x08) != 0) doors += "ПП ";
            if ((d[1] & 0x10) != 0) doors += "багажник ";
            if ((d[2] & 0x02) != 0) doors += "капот ";
            if ((d[7] & 0x02) != 0) doors += "люк ";
            if (doors.length() == 0) doors = "кузов закрыт";
            String text = "зажигание " + (ignition ? "вкл" : "выкл") + ", " + doors.trim();
            if (!text.equals(lastBody)) {
                lastBody = text;
                AppLog.line(context, "CAN кузов: " + text);
            }
            return;
        }
        if (id == 0x553 && d.length >= 8) {
            String doors = "";
            if ((d[3] & 0x01) != 0) doors += "ЛЗ ";
            if ((d[2] & 0x80) != 0) doors += "ПЗ ";
            String text = doors.length() > 0 ? doors.trim() : "задние закрыты";
            if (!text.equals(lastRearDoors)) {
                lastRearDoors = text;
                AppLog.line(context, "CAN двери: " + text);
            }
            return;
        }
        if (id == 0x169 && d.length >= 1) {
            boolean reverse = (d[0] & 0x0f) == 0x07;
            String text = reverse ? "вкл" : "выкл";
            if (!text.equals(lastReverse)) {
                lastReverse = text;
                AppLog.line(context, "CAN задний ход: " + text);
            }
            return;
        }
        if ((id == 0x131 || id == 0x132 || id == 0x134) && d.length > 0) {
            String text = "0x" + Integer.toHexString(id).toUpperCase() + " " + CanbusControl.hex(d);
            if (!text.equals(lastClimate)) {
                lastClimate = text;
                AppLog.line(context, "CAN климат raw: " + text);
            }
            return;
        }
        if ((id == 0x4F4 || id == 0x2B0) && d.length > 0) {
            String text = "0x" + Integer.toHexString(id).toUpperCase() + " " + CanbusControl.hex(d);
            if (!text.equals(lastParking)) {
                lastParking = text;
                AppLog.line(context, "CAN парковка/руль raw: " + text);
            }
        }
    }

    private static void status(Context context, String value) {
        long now = System.currentTimeMillis();
        if (!value.equals(lastStatus) || now - lastStatusAt > 5000L) {
            lastStatus = value;
            lastStatusAt = now;
            ObdState.status(context, value, true);
        }
    }
}
