package com.pinmi.react.printer.adapter;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Base64;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.graphics.Rect;
import android.graphics.Matrix;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.Paint;
import android.graphics.Canvas;

/**
 * Created by xiesubin on 2017/9/22.
 */

public class NetPrinterAdapter implements PrinterAdapter {
    private static NetPrinterAdapter mInstance;
    private ReactApplicationContext mContext;
    private String LOG_TAG = "RNNetPrinter";
    private NetPrinterDevice mNetDevice;

    // {TODO- support other ports later}
    // private int[] PRINTER_ON_PORTS = {515, 3396, 9100, 9303};

    private int[] PRINTER_ON_PORTS = { 9100 };
    private static final String EVENT_SCANNER_RESOLVED = "scannerResolved";
    private static final String EVENT_SCANNER_RUNNING = "scannerRunning";

    private static int[] p0 = new int[] { 0, 128 };
    private static int[] p1 = new int[] { 0, 64 };
    private static int[] p2 = new int[] { 0, 32 };
    private static int[] p3 = new int[] { 0, 16 };
    private static int[] p4 = new int[] { 0, 8 };
    private static int[] p5 = new int[] { 0, 4 };
    private static int[] p6 = new int[] { 0, 2 };

    private Socket mSocket;

    private boolean isRunning = false;

    private NetPrinterAdapter() {

    }

    public static NetPrinterAdapter getInstance() {
        if (mInstance == null) {
            mInstance = new NetPrinterAdapter();

        }
        return mInstance;
    }

    @Override
    public void init(ReactApplicationContext reactContext, Callback successCallback, Callback errorCallback) {
        this.mContext = reactContext;
        successCallback.invoke();
    }

    @Override
    public List<PrinterDevice> getDeviceList(Callback errorCallback) {
        // errorCallback.invoke("do not need to invoke get device list for net
        // printer");
        // Use emitter instancee get devicelist to non block main thread
        this.scan();
        List<PrinterDevice> printerDevices = new ArrayList<>();
        return printerDevices;
    }

    private void scan() {
        if (isRunning)
            return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    isRunning = true;
                    emitEvent(EVENT_SCANNER_RUNNING, isRunning);

                    WifiManager wifiManager = (WifiManager) mContext.getApplicationContext()
                            .getSystemService(Context.WIFI_SERVICE);
                    String ipAddress = ipToString(wifiManager.getConnectionInfo().getIpAddress());
                    WritableArray array = Arguments.createArray();

                    String prefix = ipAddress.substring(0, ipAddress.lastIndexOf('.') + 1);
                    int suffix = Integer
                            .parseInt(ipAddress.substring(ipAddress.lastIndexOf('.') + 1, ipAddress.length()));

                    for (int i = 0; i <= 255; i++) {
                        if (i == suffix)
                            continue;
                        ArrayList<Integer> ports = getAvailablePorts(prefix + i);
                        if (!ports.isEmpty()) {
                            WritableMap payload = Arguments.createMap();

                            payload.putString("host", prefix + i);
                            payload.putInt("port", 9100);

                            array.pushMap(payload);
                        }
                    }

                    emitEvent(EVENT_SCANNER_RESOLVED, array);

                } catch (NullPointerException ex) {
                    Log.i(LOG_TAG, "No connection");
                } finally {
                    isRunning = false;
                    emitEvent(EVENT_SCANNER_RUNNING, isRunning);
                }
            }
        }).start();
    }

    private void emitEvent(String eventName, Object data) {
        if (mContext != null) {
            mContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, data);
        }
    }

    private ArrayList<Integer> getAvailablePorts(String address) {
        ArrayList<Integer> ports = new ArrayList<>();
        for (int port : PRINTER_ON_PORTS) {
            if (crunchifyAddressReachable(address, port))
                ports.add(port);
        }
        return ports;
    }

    private static boolean crunchifyAddressReachable(String address, int port) {
        try {

            try (Socket crunchifySocket = new Socket()) {
                // Connects this socket to the server with a specified timeout value.
                crunchifySocket.connect(new InetSocketAddress(address, port), 100);
            }
            // Return true if connection successful
            return true;
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    private String ipToString(int ip) {
        return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
    }

    @Override
    public void selectDevice(PrinterDeviceId printerDeviceId, Callback sucessCallback, Callback errorCallback) {
        NetPrinterDeviceId netPrinterDeviceId = (NetPrinterDeviceId) printerDeviceId;

        if (this.mSocket != null && !this.mSocket.isClosed()
                && mNetDevice.getPrinterDeviceId().equals(netPrinterDeviceId)) {
            Log.i(LOG_TAG, "already selected device, do not need repeat to connect");
            sucessCallback.invoke(this.mNetDevice.toRNWritableMap());
            return;
        }

        try {
            Socket socket = new Socket(netPrinterDeviceId.getHost(), netPrinterDeviceId.getPort());
            if (socket.isConnected()) {
                closeConnectionIfExists();
                this.mSocket = socket;
                this.mNetDevice = new NetPrinterDevice(netPrinterDeviceId.getHost(), netPrinterDeviceId.getPort());
                sucessCallback.invoke(this.mNetDevice.toRNWritableMap());
            } else {
                errorCallback.invoke("unable to build connection with host: " + netPrinterDeviceId.getHost()
                        + ", port: " + netPrinterDeviceId.getPort());
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            errorCallback.invoke("failed to connect printer: " + e.getMessage());
        }
    }

    @Override
    public void closeConnectionIfExists() {
        if (this.mSocket != null) {
            if (!this.mSocket.isClosed()) {
                try {
                    this.mSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            this.mSocket = null;

        }
    }

    @Override
    public void printRawData(String rawBase64Data, Callback errorCallback) {
        if (this.mSocket == null) {
            errorCallback.invoke("bluetooth connection is not built, may be you forgot to connectPrinter");
            return;
        }
        final String rawData = rawBase64Data;
        final Socket socket = this.mSocket;
        Log.v(LOG_TAG, "start to print raw data " + rawBase64Data);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] bytes = Base64.decode(rawData, Base64.DEFAULT);
                    OutputStream printerOutputStream = socket.getOutputStream();
                    printerOutputStream.write(bytes, 0, bytes.length);
                    printerOutputStream.flush();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "failed to print data" + rawData);
                    e.printStackTrace();
                }
            }
        }).start();

    }

    @Override
    public void printImage(String image, int width, boolean cutPaper, boolean openCashDrawer, Callback successCallback,
            Callback errorCallback) {
        if (this.mSocket == null) {
            errorCallback.invoke("bluetooth connection is not built, may be you forgot to connectPrinter");
            return;
        }
        Log.v(LOG_TAG, "image is:  " + image);
        final boolean cut = cutPaper;
        final boolean kickDrawer = openCashDrawer;
        final Callback success = successCallback;
        byte[] decodeBase64ImageString = Base64.decode(image, Base64.DEFAULT);
        Bitmap bitmapImage = BitmapFactory.decodeByteArray(decodeBase64ImageString, 0, decodeBase64ImageString.length);
        Log.d("NetPrinterModule", "decodeBase64ImageString is:  " + decodeBase64ImageString + " and bitmapImage: "
                + bitmapImage + " and width is: " + width);

        if (bitmapImage != null) {
            bitmapImage = resizeImage(bitmapImage, width, false);
            final byte[] cutPrinter = selectCutPagerModerAndCutPager(66, 1);
            final byte[] data = rasterBmpToSendData(0, bitmapImage, width);
            final byte[] kick = new byte[] { 27, 112, 48, 55, 121 };
            final byte[] alignCenter = new byte[] { 27, 97, 1 };
            final Socket socket = this.mSocket;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        OutputStream printerOutputStream = socket.getOutputStream();
                        printerOutputStream.write(data, 0, data.length);
                        printerOutputStream.write(alignCenter, 0, alignCenter.length);
                        if (cut == true) {
                            printerOutputStream.write(cutPrinter, 0, cutPrinter.length);
                        }
                        if (kickDrawer == true) {
                            printerOutputStream.write(kick, 0, kick.length);
                        }
                        printerOutputStream.flush();
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                        success.invoke("Print successfully");
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "failed to print image");
                        e.printStackTrace();
                    }
                }
            }).start();
        } else {
            Log.d("NetPrinterModule", "bitmapImage is null");
            return;
        }

    }

    @Override
    public void openCashDrawer(Callback successCallback, Callback errorCallback) {
        if (this.mSocket == null) {
            errorCallback.invoke("bluetooth connection is not built, may be you forgot to connectPrinter");
            return;
        }
        final Callback success = successCallback;
        final byte[] kick = new byte[] { 27, 112, 48, 55, 121 };
        final Socket socket = this.mSocket;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OutputStream printerOutputStream = socket.getOutputStream();
                    printerOutputStream.write(kick, 0, kick.length);
                    printerOutputStream.flush();
                    success.invoke("Open cash successfully");
                } catch (IOException e) {
                    Log.e(LOG_TAG, "failed to print image");
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static byte[] initImageCommand(int bytesByLine, int bitmapHeight) {
        int xH = bytesByLine / 256, xL = bytesByLine - (xH * 256), yH = bitmapHeight / 256,
                yL = bitmapHeight - (yH * 256);

        byte[] imageBytes = new byte[8 + bytesByLine * bitmapHeight];
        System.arraycopy(new byte[] { 0x1D, 0x76, 0x30, 0x00, (byte) xL, (byte) xH, (byte) yL, (byte) yH }, 0,
                imageBytes, 0, 8);
        return imageBytes;
    }

    public static byte[] rasterBmpToSendData(final int m, final Bitmap mBitmap, final int pagewidth) {
        int bitmapWidth = mBitmap.getWidth(), bitmapHeight = mBitmap.getHeight(),
                bytesByLine = (int) Math.ceil(((float) bitmapWidth) / 8f);

        byte[] imageBytes = initImageCommand(bytesByLine, bitmapHeight);

        int i = 8;
        for (int posY = 0; posY < bitmapHeight; posY++) {
            for (int j = 0; j < bitmapWidth; j += 8) {
                StringBuilder stringBinary = new StringBuilder();
                for (int k = 0; k < 8; k++) {
                    int posX = j + k;
                    if (posX < bitmapWidth) {
                        int color = mBitmap.getPixel(posX, posY), r = (color >> 16) & 0xff, g = (color >> 8) & 0xff,
                                b = color & 0xff;

                        if (r > 160 && g > 160 && b > 160) {
                            stringBinary.append("0");
                        } else {
                            stringBinary.append("1");
                        }
                    } else {
                        stringBinary.append("0");
                    }
                }
                imageBytes[i++] = (byte) Integer.parseInt(stringBinary.toString(), 2);
            }
        }

        return imageBytes;
    }

    private static Bitmap toGrayscale(final Bitmap bmpOriginal) {
        final int height = bmpOriginal.getHeight();
        final int width = bmpOriginal.getWidth();
        final Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        final Canvas c = new Canvas(bmpGrayscale);
        final Paint paint = new Paint();
        final ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0.0f);
        final ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter((ColorFilter) f);
        c.drawBitmap(bmpOriginal, 0.0f, 0.0f, paint);
        return bmpGrayscale;
    }

    private static Bitmap convertGreyImgByFloyd(final Bitmap img) {
        final int width = img.getWidth();
        final int height = img.getHeight();
        final int[] pixels = new int[width * height];
        img.getPixels(pixels, 0, width, 0, 0, width, height);
        final int[] gray = new int[height * width];
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                final int grey = pixels[width * i + j];
                final int red = (grey & 0xFF0000) >> 16;
                gray[width * i + j] = red;
            }
        }
        int e = 0;
        for (int k = 0; k < height; ++k) {
            for (int l = 0; l < width; ++l) {
                final int g = gray[width * k + l];
                if (g >= 128) {
                    pixels[width * k + l] = -1;
                    e = g - 255;
                } else {
                    pixels[width * k + l] = -16777216;
                    e = g - 0;
                }
                if (l < width - 1 && k < height - 1) {
                    final int[] array = gray;
                    final int n = width * k + l + 1;
                    array[n] += 3 * e / 8;
                    final int[] array2 = gray;
                    final int n2 = width * (k + 1) + l;
                    array2[n2] += 3 * e / 8;
                    final int[] array3 = gray;
                    final int n3 = width * (k + 1) + l + 1;
                    array3[n3] += e / 4;
                } else if (l == width - 1 && k < height - 1) {
                    final int[] array4 = gray;
                    final int n4 = width * (k + 1) + l;
                    array4[n4] += 3 * e / 8;
                } else if (l < width - 1 && k == height - 1) {
                    final int[] array5 = gray;
                    final int n5 = width * k + l + 1;
                    array5[n5] += e / 4;
                }
            }
        }
        final Bitmap mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        mBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return mBitmap;
    }

    private static byte[] getbmpdata(final int[] b, final int w, final int h) {
        final int n = (w + 7) / 8;
        final byte[] data = new byte[n * h];
        final byte mask = 1;
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < n * 8; ++x) {
                if (x < w) {
                    if ((b[y * w + x] & 0xFF0000) >> 16 != 0) {
                        final byte[] array = data;
                        final int n2 = y * n + x / 8;
                        array[n2] |= (byte) (mask << 7 - x % 8);
                    }
                } else if (x >= w) {
                    final byte[] array2 = data;
                    final int n3 = y * n + x / 8;
                    array2[n3] |= (byte) (mask << 7 - x % 8);
                }
            }
        }
        for (int i = 0; i < data.length; ++i) {
            data[i] ^= -1;
        }
        return data;
    }

    public static byte[] setAbsolutePrintPosition(final int m, final int n) {
        final byte[] data = { 27, 36, (byte) m, (byte) n };
        return data;
    }

    public static byte[] initializePrinter() {
        final byte[] data = { 27, 64 };
        return data;
    }

    public static byte[] selectCutPagerModerAndCutPager(final int m, final int n) {
        if (m != 66) {
            return new byte[0];
        }
        final byte[] data = { 29, 86, (byte) m, (byte) n };
        return data;
    }

    public static Bitmap resizeImage(Bitmap bitmap, int w, boolean ischecked) {

        Bitmap BitmapOrg = bitmap;
        Bitmap resizedBitmap = null;
        int width = BitmapOrg.getWidth();
        int height = BitmapOrg.getHeight();
        if (width <= w) {
            return bitmap;
        }
        if (!ischecked) {
            int newWidth = w;
            int newHeight = height * w / width;

            float scaleWidth = ((float) newWidth) / width;
            float scaleHeight = ((float) newHeight) / height;

            Matrix matrix = new Matrix();
            matrix.postScale(scaleWidth, scaleHeight);
            // if you want to rotate the Bitmap
            // matrix.postRotate(45);
            resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width, height, matrix, true);
        } else {
            resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, w, height);
        }

        return resizedBitmap;
    }
}
