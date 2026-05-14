package kia.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;

final class LogOverlayView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    LogOverlayView(Context context) {
        super(context);
        setWillNotDraw(false);
        setClickable(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float top = h * 0.5f;
        float pad = Math.max(18f, w / 90f);
        float lineGap = Math.max(4f, h / 260f);
        float textSize = Math.max(16f, Math.min(26f, w / 82f));

        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);
        paint.clearShadowLayer();
        paint.setColor(0xb8a50000);
        canvas.drawRect(0, top, w, h, paint);

        paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        paint.setTextSize(textSize);
        paint.setColor(0xffffffff);
        paint.setShadowLayer(4f, 1.5f, 1.5f, 0xff000000);
        paint.setTextAlign(Paint.Align.LEFT);

        float y = top + pad + textSize;
        y = drawWrapped(canvas, "ЖУРНАЛ | " + AppLog.usb(), pad, y, w - pad * 2f, lineGap, 1);
        y = drawWrapped(canvas, AppLog.media(), pad, y, w - pad * 2f, lineGap, 1);
        y = drawWrapped(canvas, AppLog.nav(), pad, y, w - pad * 2f, lineGap, 1);

        paint.setTypeface(Typeface.MONOSPACE);
        paint.setTextSize(Math.max(14f, textSize - 2f));
        String[] lines = tailLines(AppLog.text(), 18);
        int maxLines = Math.max(3, (int) ((h - y - pad) / (paint.getTextSize() + lineGap)));
        int start = Math.max(0, lines.length - maxLines);
        for (int i = start; i < lines.length && y < h - pad; i++) {
            y = drawWrapped(canvas, lines[i], pad, y, w - pad * 2f, lineGap, 1);
        }

        postInvalidateDelayed(500);
    }

    private float drawWrapped(Canvas canvas, String value, float x, float y, float maxWidth, float gap, int maxLines) {
        if (value == null || value.length() == 0) value = "-";
        String text = value.trim();
        int drawn = 0;
        while (text.length() > 0 && drawn < maxLines) {
            int end = fitEnd(text, maxWidth);
            String line = text.substring(0, end).trim();
            if (drawn == maxLines - 1 && end < text.length() && line.length() > 3) {
                line = ellipsize(line, maxWidth);
            }
            canvas.drawText(line, x, y, paint);
            y += paint.getTextSize() + gap;
            drawn++;
            text = text.substring(end).trim();
        }
        return y;
    }

    private int fitEnd(String text, float maxWidth) {
        if (paint.measureText(text) <= maxWidth) return text.length();
        int low = 1;
        int high = text.length();
        while (low < high) {
            int mid = (low + high + 1) / 2;
            if (paint.measureText(text, 0, mid) <= maxWidth) low = mid;
            else high = mid - 1;
        }
        int space = text.lastIndexOf(' ', low);
        return Math.max(1, space > 8 ? space : low);
    }

    private String ellipsize(String text, float maxWidth) {
        String suffix = "...";
        while (text.length() > 1 && paint.measureText(text + suffix) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + suffix;
    }

    private String[] tailLines(String value, int maxLines) {
        if (value == null || value.length() == 0) return new String[]{"Журнал пуст"};
        String[] all = value.split("\\n");
        if (all.length <= maxLines) return all;
        String[] tail = new String[maxLines];
        System.arraycopy(all, all.length - maxLines, tail, 0, maxLines);
        return tail;
    }
}
