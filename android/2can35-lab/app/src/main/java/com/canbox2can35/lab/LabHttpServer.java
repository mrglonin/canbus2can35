package com.canbox2can35.lab;

import android.content.Context;
import android.content.res.AssetManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LabHttpServer implements UsbCdcManager.LineListener {
    private final Context context;
    private final AssetManager assets;
    private final UsbCdcManager usb;
    private final int port;
    private final Object lock = new Object();
    private final ArrayDeque<JSONObject> uartEvents = new ArrayDeque<>();
    private final ArrayDeque<JSONObject> canEvents = new ArrayDeque<>();
    private volatile boolean serving;
    private ServerSocket serverSocket;
    private Thread serverThread;
    private boolean running;
    private String mode = "idle";
    private boolean bridgeEnabled;
    private long mcanFrames;
    private long ccanFrames;
    private long startedMs;
    private long lastFrameMs;

    public LabHttpServer(Context context, UsbCdcManager usb, int port) {
        this.context = context.getApplicationContext();
        this.assets = context.getAssets();
        this.usb = usb;
        this.port = port;
        this.usb.setLineListener(this);
    }

    public void start() {
        serving = true;
        serverThread = new Thread(this::serve, "2can35-http");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    public void stop() {
        serving = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
        }
    }

    private void serve() {
        try {
            serverSocket = new ServerSocket(port);
            while (serving) {
                Socket socket = serverSocket.accept();
                Thread t = new Thread(() -> handle(socket), "2can35-http-client");
                t.setDaemon(true);
                t.start();
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    public void onLine(String line) {
        try {
            long ms = System.currentTimeMillis();
            synchronized (lock) {
                lastFrameMs = ms;
                if (line.startsWith("U ")) {
                    JSONObject event = new JSONObject();
                    event.put("ms", ms);
                    event.put("raw", line.substring(2).trim().toUpperCase(Locale.ROOT));
                    event.put("cmd", decodeUartCmd(event.optString("raw")));
                    event.put("text", decodeUartText(event.optString("raw")));
                    event.put("valid", true);
                    push(uartEvents, event, 80);
                } else if (line.matches("^[01] [tT].*")) {
                    JSONObject event = parseSlcanLine(line, ms);
                    if (event != null) {
                        int ch = event.optInt("channel", 0);
                        if (ch == 0) ccanFrames++;
                        if (ch == 1) mcanFrames++;
                        push(canEvents, event, 160);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void push(ArrayDeque<JSONObject> queue, JSONObject item, int limit) {
        queue.addFirst(item);
        while (queue.size() > limit) queue.removeLast();
    }

    private JSONObject parseSlcanLine(String line, long ms) {
        try {
            String[] parts = line.trim().split("\\s+", 2);
            int channel = Integer.parseInt(parts[0]);
            String frame = parts[1];
            boolean ext = frame.charAt(0) == 'T';
            int idLen = ext ? 8 : 3;
            int id = Integer.parseInt(frame.substring(1, 1 + idLen), 16);
            int dlc = Integer.parseInt(frame.substring(1 + idLen, 2 + idLen), 16);
            String data = frame.substring(2 + idLen);
            JSONObject out = new JSONObject();
            out.put("ms", ms);
            out.put("channel", channel);
            out.put("bus", channel == 1 ? "M-CAN" : "C-CAN");
            out.put("id_hex", String.format(Locale.ROOT, "0x%03X", id));
            out.put("dlc", dlc);
            out.put("data", spaced(data));
            out.put("extended", ext);
            return out;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void handle(Socket socket) {
        try (Socket s = socket) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            String request = reader.readLine();
            if (request == null) return;
            String[] head = request.split(" ");
            String method = head.length > 0 ? head[0] : "GET";
            String rawPath = head.length > 1 ? head[1] : "/";
            int contentLength = 0;
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                String lower = line.toLowerCase(Locale.ROOT);
                if (lower.startsWith("content-length:")) {
                    contentLength = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
                }
            }
            char[] bodyChars = new char[contentLength];
            int read = 0;
            while (read < contentLength) {
                int n = reader.read(bodyChars, read, contentLength - read);
                if (n < 0) break;
                read += n;
            }
            String body = new String(bodyChars, 0, read);
            String path = rawPath.split("\\?", 2)[0];
            if ("/api/events".equals(path)) {
                handleEvents(s.getOutputStream());
            } else if (path.startsWith("/api/")) {
                handleApi(s.getOutputStream(), method, path, body);
            } else {
                serveAsset(s.getOutputStream(), path);
            }
        } catch (Exception ignored) {
        }
    }

    private void handleEvents(OutputStream out) throws Exception {
        writeHeaders(out, 200, "text/event-stream; charset=utf-8", false);
        while (serving) {
            JSONObject event = new JSONObject();
            event.put("summary", summary());
            out.write(("data: " + event + "\n\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
                break;
            }
        }
    }

    private void handleApi(OutputStream out, String method, String path, String body) throws Exception {
        JSONObject req = body == null || body.isEmpty() ? new JSONObject() : new JSONObject(body);
        JSONObject response = new JSONObject();
        switch (path) {
            case "/api/status":
                response.put("summary", summary());
                response.put("auto_port", "/dev/ttyACM0");
                response.put("gsusb_present", false);
                json(out, response);
                return;
            case "/api/commands":
                json(out, commands());
                return;
            case "/api/learned":
                json(out, new JSONArray());
                return;
            case "/api/export":
                response.put("summary", summary());
                response.put("can_recent", new JSONArray(canEvents));
                response.put("uart_recent", new JSONArray(uartEvents));
                json(out, response);
                return;
            case "/api/export/can/m":
            case "/api/export/can/c":
                json(out, new JSONArray(canEvents));
                return;
            case "/api/export/uart":
            case "/api/export/learned-table":
                json(out, new JSONArray(uartEvents));
                return;
            case "/api/log/start":
                running = true;
                mode = req.optString("mode", "lab");
                startedMs = System.currentTimeMillis();
                usb.openFirst();
                response.put("ok", true);
                response.put("tx_control", usb.label());
                json(out, response);
                return;
            case "/api/log/stop":
                running = false;
                mode = "idle";
                response.put("ok", true);
                json(out, response);
                return;
            case "/api/reset":
                synchronized (lock) {
                    canEvents.clear();
                    uartEvents.clear();
                    mcanFrames = 0;
                    ccanFrames = 0;
                }
                response.put("summary", summary());
                json(out, response);
                return;
            case "/api/bridge":
                bridgeEnabled = !bridgeEnabled;
                response.put("bridge", bridge());
                json(out, response);
                return;
            case "/api/lab/send":
                String command = req.optString("command", "");
                boolean sent = sendLabCommand(command);
                response.put("ok", sent);
                response.put("uart_hex", command);
                json(out, response);
                return;
            case "/api/mode":
                String target = req.optString("mode", "normal");
                mode = target;
                sendLabCommand("mode " + target);
                response.put("ok", true);
                response.put("stdout", "android local mode command sent");
                json(out, response);
                return;
            case "/api/send/display":
                int sentFrames = sendDisplayBundle(req);
                response.put("ok", true);
                response.put("sent", sentFrames);
                json(out, response);
                return;
            case "/api/can/send":
                response.put("ok", sendCan(req));
                response.put("sent", 1);
                json(out, response);
                return;
            case "/api/can/sweep":
                response.put("ok", false);
                response.put("error", "sweep disabled in Android MVP");
                json(out, response);
                return;
            case "/api/learn/start":
                response.put("ok", true);
                response.put("action_id", req.optString("action_id"));
                json(out, response);
                return;
            case "/api/learn/stop":
                response.put("result", emptyLearnResult());
                json(out, response);
                return;
            case "/api/learn/save":
            case "/api/uart/verify":
                response.put("ok", true);
                response.put("item", req);
                json(out, response);
                return;
            default:
                response.put("error", "unknown api " + method + " " + path);
                writeJson(out, 404, response);
        }
    }

    private JSONObject summary() throws Exception {
        synchronized (lock) {
            long now = System.currentTimeMillis();
            double seconds = Math.max(1.0, (now - Math.max(startedMs, now - 1000)) / 1000.0);
            JSONObject modules = new JSONObject();
            modules.put("mcan_frames", mcanFrames);
            modules.put("ccan_frames", ccanFrames);
            modules.put("mcan_fps", running ? mcanFrames / seconds : 0);
            modules.put("ccan_fps", running ? ccanFrames / seconds : 0);
            modules.put("uart_rx", usb.rxCount());
            modules.put("uart_tx", usb.txCount());
            modules.put("active_mode", usb.isOpen() ? usb.label() : "no USB");

            JSONObject session = new JSONObject();
            session.put("running", running);
            session.put("mode", mode);
            session.put("device", usb.label());

            JSONObject out = new JSONObject();
            out.put("session", session);
            out.put("module_stats", modules);
            out.put("bridge", bridge());
            out.put("semantic", semantic());
            out.put("semantic_events", new JSONArray());
            out.put("uart_state", uartState());
            out.put("uart_command_counts", new JSONArray());
            out.put("uart_events", new JSONArray(uartEvents));
            out.put("android_notifications", NotificationStore.snapshot());
            out.put("recent", new JSONArray(canEvents));
            JSONObject learn = new JSONObject();
            learn.put("active", false);
            out.put("learn", learn);
            out.put("last_frame_age", lastFrameMs == 0 ? null : (now - lastFrameMs) / 1000.0);
            return out;
        }
    }

    private JSONObject bridge() throws Exception {
        JSONObject bridge = new JSONObject();
        bridge.put("enabled", bridgeEnabled);
        bridge.put("tx_count", usb.txCount());
        bridge.put("label", bridgeEnabled ? "CAN→UART вкл" : "CAN→UART выкл");
        return bridge;
    }

    private JSONArray semantic() throws Exception {
        JSONArray arr = new JSONArray();
        arr.put(semanticItem("ignition", "Зажигание", "ожидание", "android"));
        arr.put(semanticItem("media_source", "Источник данных", running ? "live" : "offline", "android"));
        arr.put(semanticItem("usb", "USB", usb.isOpen() ? usb.label() : "no USB", "android"));
        return arr;
    }

    private JSONObject semanticItem(String key, String label, String value, String source) throws Exception {
        JSONObject item = new JSONObject();
        item.put("key", key);
        item.put("label", label);
        item.put("value", value);
        item.put("source", source);
        return item;
    }

    private JSONObject uartState() throws Exception {
        JSONObject state = new JSONObject();
        JSONObject last = uartEvents.peekFirst();
        state.put("source", "нет данных");
        state.put("source_code", "-");
        state.put("track", "-");
        state.put("play_time", "-");
        state.put("radio", "-");
        state.put("nav", "-");
        state.put("bt", "-");
        state.put("hu_time", "-");
        state.put("power", usb.isOpen() ? "USB connected" : "offline");
        state.put("last_valid", last == null ? "-" : last.optString("raw"));
        state.put("valid_count", uartEvents.size());
        state.put("invalid_count", 0);
        JSONArray notifications = NotificationStore.snapshot();
        state.put("android_notifications", notifications);
        for (int i = notifications.length() - 1; i >= 0; i--) {
            JSONObject item = notifications.optJSONObject(i);
            if (item == null) continue;
            String pkg = item.optString("package", "");
            String title = item.optString("title", "");
            String text = item.optString("text", "");
            String combined = (pkg + " " + title + " " + text).toLowerCase(Locale.ROOT);
            if (combined.contains("music") || combined.contains("spotify") || combined.contains("player") ||
                    combined.contains("музык") || combined.contains("трек")) {
                state.put("source", pkg);
                state.put("track", title.isEmpty() ? text : title);
                break;
            }
            if (combined.contains("nav") || combined.contains("maps") || combined.contains("яндекс") ||
                    combined.contains("навиг")) {
                state.put("nav", title.isEmpty() ? text : title + " " + text);
            }
        }
        return state;
    }

    private JSONObject commands() throws Exception {
        JSONObject out = new JSONObject();
        out.put("actions", actions());
        out.put("test_plan", actions());
        out.put("safe_uart_tests", safeUart());
        out.put("hu_to_canbox_examples", new JSONArray());
        out.put("raise_matrix", csvRows("raise_rzc_korea_uart_matrix.csv", 120));
        out.put("can_matrix", csvRows("can_function_matrix.csv", 160));
        out.put("uart_candidate_tests", uartCandidates());
        out.put("demo_scenarios", demoScenarios());
        return out;
    }

    private JSONArray actions() throws Exception {
        JSONArray arr = new JSONArray();
        String[][] rows = {
                {"baseline", "0. База", "Покой 10 секунд", "ничего не нажимай, это шумовая база"},
                {"door_driver", "1. Кузов", "Дверь водительская", "открыть/закрыть 3 раза"},
                {"sunroof", "1. Кузов", "Люк", "закрыт -> открыт -> закрыт 3 раза"},
                {"reverse", "3. Движение", "Задний ход", "P -> R -> P на тормозе"},
                {"front_defog", "4. Климат", "Обдув лобового", "включить/выключить 5 раз"},
                {"heated_wheel", "4. Климат", "Подогрев руля", "включить/выключить"},
                {"hu_media_usb", "8. UART Raise", "Магнитола USB media", "включить USB источник"},
                {"hu_navigation", "8. UART Raise", "Навигация", "маневр/улица/компас"}
        };
        for (String[] r : rows) {
            JSONObject item = new JSONObject();
            item.put("id", r[0]);
            item.put("group", r[1]);
            item.put("name", r[2]);
            item.put("hint", r[3]);
            item.put("status", "pending");
            arr.put(item);
        }
        return arr;
    }

    private JSONArray safeUart() throws Exception {
        JSONArray arr = new JSONArray();
        addSafe(arr, "body_closed", "Кузов: все закрыто", "FD 05 05 00 00 0A");
        addSafe(arr, "door_lf", "Дверь водитель открыта", "FD 05 05 01 00 0B");
        addSafe(arr, "sunroof", "Люк открыт candidate", "FD 05 05 40 00 4A");
        addSafe(arr, "reverse_on", "Задний ход on", "FD 06 7D 06 02 00 8B");
        addSafe(arr, "reverse_off", "Задний ход off", "FD 06 7D 06 00 00 89");
        addSafe(arr, "climate_popup", "Климат popup demo", "FD 08 03 16 16 03 24 00 5E");
        return arr;
    }

    private void addSafe(JSONArray arr, String id, String name, String frame) throws Exception {
        JSONObject item = new JSONObject();
        item.put("id", id);
        item.put("name", name);
        item.put("frame", frame);
        item.put("lab", "u" + frame.replace(" ", ""));
        arr.put(item);
    }

    private JSONArray uartCandidates() throws Exception {
        JSONArray arr = new JSONArray();
        JSONArray rows = csvRows("raise_uart_full_worklist.csv", 80);
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null) continue;
            JSONObject item = new JSONObject();
            item.put("id", row.optString("id", "raise_" + i));
            item.put("group", row.optString("group", "Raise"));
            item.put("protocol", row.optString("protocol", "raise"));
            item.put("direction", row.optString("direction", ""));
            item.put("name", row.optString("name", row.optString("meaning", "-")));
            item.put("frame", row.optString("frame", row.optString("payload", "")));
            item.put("lab", "u" + item.optString("frame").replace(" ", ""));
            item.put("decoded", row.optString("decoded", row.optString("note", "")));
            item.put("status", row.optString("status", "candidate"));
            arr.put(item);
        }
        return arr;
    }

    private JSONArray demoScenarios() throws Exception {
        JSONArray arr = new JSONArray();
        String[] names = {"walkaround", "parking", "climate", "media", "stress"};
        for (String name : names) {
            JSONObject item = new JSONObject();
            item.put("id", name);
            item.put("name", name);
            arr.put(item);
        }
        return arr;
    }

    private JSONArray csvRows(String asset, int limit) {
        JSONArray arr = new JSONArray();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(assets.open(asset), StandardCharsets.UTF_8))) {
            String header = br.readLine();
            if (header == null) return arr;
            List<String> keys = parseCsv(header);
            String line;
            while ((line = br.readLine()) != null && arr.length() < limit) {
                List<String> values = parseCsv(line);
                JSONObject row = new JSONObject();
                for (int i = 0; i < keys.size(); i++) row.put(keys.get(i), i < values.size() ? values.get(i) : "");
                arr.put(row);
            }
        } catch (Exception ignored) {
        }
        return arr;
    }

    private List<String> parseCsv(String line) {
        ArrayList<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quote && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    quote = !quote;
                }
            } else if (c == ',' && !quote) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }

    private JSONObject emptyLearnResult() throws Exception {
        JSONObject result = new JSONObject();
        result.put("action_id", "android_mvp");
        result.put("action_name", "Android MVP");
        result.put("target_repeats", 5);
        result.put("detected_changes", 0);
        result.put("noise_hidden", 0);
        result.put("candidates", new JSONArray());
        result.put("uart", new JSONArray(uartEvents));
        return result;
    }

    private boolean sendLabCommand(String command) {
        String line = command == null ? "" : command.trim();
        if (line.startsWith("u") || line.startsWith("U")) line = "U " + spaced(line.substring(1));
        return usb.writeLine(line);
    }

    private boolean sendCan(JSONObject req) throws Exception {
        int channel = req.optInt("channel", 1);
        String idText = req.optString("id", "0x114").replace("0x", "").replace("0X", "");
        String data = req.optString("data", "").replace(" ", "");
        int id = Integer.parseInt(idText, 16);
        int dlc = Math.min(8, data.length() / 2);
        String frame = channel + " t" + String.format(Locale.ROOT, "%03X", id & 0x7FF) + Integer.toHexString(dlc).toUpperCase(Locale.ROOT) + data.substring(0, dlc * 2).toUpperCase(Locale.ROOT);
        return usb.writeLine(frame);
    }

    private int sendDisplayBundle(JSONObject req) throws Exception {
        int channel = "c".equalsIgnoreCase(req.optString("bus")) ? 0 : 1;
        String scenario = req.optString("scenario", "full");
        int count = 0;
        count += sendFrame(channel, 0x114, "0B 21 FF FF FF FF E1 0F") ? 1 : 0;
        count += sendFrame(channel, 0x197, "10 00 00 00 00 00 00 00") ? 1 : 0;
        if (scenario.contains("nav") || "full".equals(scenario)) {
            count += sendFrame(channel, 0x1E6, "00 00 00 00 00 00 00 20") ? 1 : 0;
            count += sendFrame(channel, 0x115, "01 00 00 01 00 50 00 50") ? 1 : 0;
            count += sendFrame(channel, 0x4BB, "10 00 00 00 00 00 00 00") ? 1 : 0;
            count += sendFrame(channel, 0x49B, "10 00 00 00 00 00 00 00") ? 1 : 0;
        }
        if (scenario.contains("music") || "full".equals(scenario)) {
            count += sendFrame(channel, 0x490, "00 00 08 20 00 00 00 00") ? 1 : 0;
        }
        return count;
    }

    private boolean sendFrame(int channel, int id, String data) throws Exception {
        JSONObject req = new JSONObject();
        req.put("channel", channel);
        req.put("id", String.format(Locale.ROOT, "0x%03X", id));
        req.put("data", data);
        return sendCan(req);
    }

    private void serveAsset(OutputStream out, String path) throws IOException {
        String asset = "/".equals(path) ? "index.html" : path.substring(1);
        asset = URLDecoder.decode(asset, "UTF-8");
        try (InputStream in = assets.open(asset)) {
            byte[] bytes = readAll(in);
            writeHeaders(out, 200, mime(asset), true);
            out.write(bytes);
        } catch (IOException notFound) {
            byte[] bytes = ("not found: " + asset).getBytes(StandardCharsets.UTF_8);
            writeHeaders(out, 404, "text/plain; charset=utf-8", true);
            out.write(bytes);
        }
    }

    private byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) >= 0) out.write(buf, 0, n);
        return out.toByteArray();
    }

    private String mime(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (path.endsWith(".css")) return "text/css; charset=utf-8";
        if (path.endsWith(".json")) return "application/json; charset=utf-8";
        if (path.endsWith(".csv")) return "text/csv; charset=utf-8";
        return "application/octet-stream";
    }

    private void json(OutputStream out, Object value) throws IOException {
        writeJson(out, 200, value);
    }

    private void writeJson(OutputStream out, int code, Object value) throws IOException {
        byte[] bytes = value.toString().getBytes(StandardCharsets.UTF_8);
        writeHeaders(out, code, "application/json; charset=utf-8", true);
        out.write(bytes);
    }

    private void writeHeaders(OutputStream out, int code, String contentType, boolean close) throws IOException {
        String text = "HTTP/1.1 " + code + " OK\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Cache-Control: no-store\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                (close ? "Connection: close\r\n" : "Connection: keep-alive\r\n") +
                "\r\n";
        out.write(text.getBytes(StandardCharsets.US_ASCII));
    }

    private String decodeUartCmd(String raw) {
        String clean = raw.replace(" ", "").toUpperCase(Locale.ROOT);
        if (clean.startsWith("FD") && clean.length() >= 6) return "FD " + clean.substring(4, 6);
        if (clean.startsWith("2E") && clean.length() >= 4) return "2E " + clean.substring(2, 4);
        return "-";
    }

    private String decodeUartText(String raw) {
        String clean = raw.replace(" ", "").toUpperCase(Locale.ROOT);
        if (clean.startsWith("FD0505")) return "Raise body flags";
        if (clean.startsWith("FD067D")) return "Raise reverse/status";
        if (clean.startsWith("FD0A09")) return "HU source/media status";
        return "UART";
    }

    private static String spaced(String hex) {
        String clean = hex == null ? "" : hex.replaceAll("[^0-9A-Fa-f]", "").toUpperCase(Locale.ROOT);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i + 1 < clean.length(); i += 2) {
            if (out.length() > 0) out.append(' ');
            out.append(clean, i, i + 2);
        }
        return out.toString();
    }
}
