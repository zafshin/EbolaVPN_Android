/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.SSLCertificateSocketFactory;
import android.os.Build;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.File;
import java.lang.Process;
import java.util.List;
import android.content.pm.ApplicationInfo;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;


import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import android.util.Base64;
import javax.net.ssl.*;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import android.os.SystemClock;
import android.util.Log;

public class VPNLaunchHelper {
    private static final String MININONPIEVPN = "nopie_openvpn";
    private static Process xray;

    private static final String MINIPIEVPN = "pie_openvpn";
    private static final String OVPNCONFIGFILE = "android.conf";
    private static String writeMiniVPN(Context context) {
        String nativeAPI = NativeUtils.getNativeAPI();
        /* Q does not allow executing binaries written in temp directory anymore */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            return new File(context.getApplicationInfo().nativeLibraryDir, "libovpnexec.so").getPath();
        String[] abis;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            abis = getSupportedABIsLollipop();
        else
            //noinspection deprecation
            abis = new String[]{Build.CPU_ABI, Build.CPU_ABI2};

        if (!nativeAPI.equals(abis[0])) {
            VpnStatus.logWarning(R.string.abi_mismatch, Arrays.toString(abis), nativeAPI);
            abis = new String[]{nativeAPI};
        }

        for (String abi : abis) {

            File vpnExecutable = new File(context.getCacheDir(), "c_" + getMiniVPNExecutableName() + "." + abi);
            if ((vpnExecutable.exists() && vpnExecutable.canExecute()) || writeMiniVPNBinary(context, abi, vpnExecutable)) {
                return vpnExecutable.getPath();
            }
        }

        throw new RuntimeException("Cannot find any execulte for this device's ABIs " + abis.toString());
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static String[] getSupportedABIsLollipop() {
        return Build.SUPPORTED_ABIS;
    }

    private static String getMiniVPNExecutableName() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            return MINIPIEVPN;
        else
            return MININONPIEVPN;
    }


    public static String[] replacePieWithNoPie(String[] mArgv) {
        mArgv[0] = mArgv[0].replace(MINIPIEVPN, MININONPIEVPN);
        return mArgv;
    }


    static String[] buildOpenvpnArgv(Context c) {
        Vector<String> args = new Vector<>();

        String binaryName = writeMiniVPN(c);
        // Add fixed paramenters
        //args.add("/data/data/de.blinkt.openvpn/lib/openvpn");
        if (binaryName == null) {
            VpnStatus.logError("Error writing minivpn binary");
            return null;
        }

        args.add(binaryName);

        args.add("--config");
        args.add(getConfigFilePath(c));

        return args.toArray(new String[args.size()]);
    }

    private static boolean writeMiniVPNBinary(Context context, String abi, File mvpnout) {
        try {
            InputStream mvpn;

            try {
                mvpn = context.getAssets().open(getMiniVPNExecutableName() + "." + abi);
            } catch (IOException errabi) {
                VpnStatus.logInfo("Failed getting assets for archicture " + abi);
                return false;
            }


            FileOutputStream fout = new FileOutputStream(mvpnout);

            byte buf[] = new byte[4096];

            int lenread = mvpn.read(buf);
            while (lenread > 0) {
                fout.write(buf, 0, lenread);
                lenread = mvpn.read(buf);
            }
            fout.close();

            if (!mvpnout.setExecutable(true)) {
                VpnStatus.logError("Failed to make OpenVPN executable");
                return false;
            }


            return true;
        } catch (IOException e) {
            VpnStatus.logException(e);
            return false;
        }

    }
    static Thread pfThread;
    public static Thread ThreadSSL;
    public static sslForwarder portforwardessl;
    public static void startXray(Context context,String sni, String host, String port){
        String appPath = context.getPackageResourcePath();
        File dataDir = context.getFilesDir();

        //copy files
//        try {
//            InputStream in = context.getAssets().open("config.json");
//            OutputStream out = new FileOutputStream(new File(context.getFilesDir(), "config.json"));
//
//            byte[] buffer = new byte[1024];
//            int read;
//            while ((read = in.read(buffer)) != -1) {
//                out.write(buffer, 0, read);
//            }
//            in.close();
//            out.flush();
//            out.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        File filee = new File(context.getFilesDir().getAbsolutePath()+ "/config.json");
//
//        if (filee.exists()) {
//            if (filee.delete()) {
//                // File deleted successfully
//            } else {
//                // Error deleting file
//            }
//        } else {
//            // File does not exist
//        }

//        try {
//            InputStream inputStream = context.getAssets().open("config.json");
//            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//            String config = "";
//            String line;
//            while ((line = reader.readLine()) != null) {
//                line = line.replace("$host$",host);
//                line = line.replace("$port$",port);
//                line = line.replace("$sni$",sni);
//                // process each line here
//                config += line + "\n";
//            }
//            try {
//                File file = new File(context.getFilesDir().getAbsolutePath()+ "/config.json");
//                FileOutputStream outputStream = new FileOutputStream(file);
//                outputStream.write(config.getBytes());
//                outputStream.close();
//            } catch (IOException e) {
//                // Handle the exception here
//            }
//        } catch (IOException e) {
//            // handle the exception here
//        }
//

//        try {
//            File file = new File(context.getFilesDir()+ "/config.json");
//            FileReader fileReader = new FileReader(file);
//            BufferedReader bufferedReader = new BufferedReader(fileReader);
//            String line;
//
//            while ((line = bufferedReader.readLine()) != null) {
//                Log.d("I", "config:"+ line);
//            }
//
//            bufferedReader.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        File directory = new File(context.getApplicationInfo().nativeLibraryDir);
//        if(directory.isDirectory()) {
//            File[] files = directory.listFiles();
//
//            for (File file : files) {
//                // Do something with each file
//                // ...
//                Log.d("I", "files:"+ file.getAbsolutePath());
//            }
//        }
        if(xray!=null)
            xray.destroy();
        try {
            String executablePath = context.getApplicationInfo().nativeLibraryDir + "/libgost.so";
            String[] command = {executablePath, "-L", "auto://:11000?log?level=error", "-F", "ss+ohttp://AEAD_CHACHA20_POLY1305:nvPIHGFWFASDe@"+host+":"+port+"?resolver=8.8.8.8,1.1.1.1,1.0.0.1&log?level=error"};
            //String[] command = {"cat", context.getFilesDir()+ "/config.json"};

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(context.getFilesDir());
            xray = processBuilder.start();
//            BufferedReader errStreamReader = new BufferedReader(new InputStreamReader(xray.getErrorStream()));
//            BufferedReader inputStreamReader = new BufferedReader(new InputStreamReader(xray.getInputStream()));
//            Thread readerThread = new Thread(() -> {
//                try {
//                    String line;
//                    while ((line = inputStreamReader.readLine()) != null) {
//                        Log.d("I", "Gost:"+ line);
//                    }
//                } catch (IOException e) {
//
//                }
//            });
//            readerThread.start();
//            Thread readerThread2 = new Thread(() -> {
//                try {
//                    String line;
//                    while ((line = errStreamReader.readLine()) != null) {
//                        Log.d("E", "Gost:"+ line);
//                    }
//                } catch (IOException e) {
//                }
//            });
//            readerThread2.start();
//            try {
//                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//                    boolean ex = xray.waitFor(3000, TimeUnit.MILLISECONDS);
//                    BufferedReader inputStreamReader = new BufferedReader(new InputStreamReader(xray.getErrorStream()));
//                    String line;
//                    while ((line = inputStreamReader.readLine()) != null) {
//                        Log.d("I", "xray:" + line);
//                    }
//                    xray.destroyForcibly();
//
//                }
//            }catch(InterruptedException e) {
//
//            }

        }catch (IOException e){
            Log.d("I", "Can not run xray.");
        }
//        Log.d("I", "app Path:"+ executablePath);
//        Log.d("I", "app Path:"+ dataDir);
    }
    public static boolean checkSocks(){
        try {
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 11000));
            Socket socket = new Socket(proxy);
            socket.connect(new InetSocketAddress("www.google.com", 80), 500);
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    public static void startOpenVpn(VpnProfile startprofile, Context context, String id, String lport, String port, String host, String fakeSNI)  {

        startXray(context,fakeSNI,host,port);
    input = id;
        if(ThreadSSL!= null)
            if(ThreadSSL.isAlive()){
                ThreadSSL.interrupt();
                try {
                    ThreadSSL.join(5000);
                }catch (InterruptedException e){}
            }
        if(pfThread!= null)
            if(pfThread.isAlive()){
                pfThread.interrupt();
                try {
                    pfThread.join(5000);
                }catch (InterruptedException e){}
            }

        long startTime = System.currentTimeMillis();
        long endTime = startTime + 6000; // 6 seconds
        while (System.currentTimeMillis() < endTime) {
            SystemClock.sleep(300);
            if (checkSocks())
                break;
        }

        portforwardessl = new sslForwarder();
        portforwardessl.idName = id;
        portforwardessl.fakeSNI = fakeSNI;
        portforwardessl.port = port;
        portforwardessl.lport = lport;
        portforwardessl.host = host;
//        Log.d("I", "Not connected");
        ThreadSSL = new Thread(portforwardessl);
        ThreadSSL.start();

        Intent startVPN = startprofile.prepareStartService(context);
        if (startVPN != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                //noinspection NewApi
                context.startForegroundService(startVPN);
            else
                context.startService(startVPN);

        }
    }

   public static String input = "";

    public static String getConfigFilePath(Context context) {
        return context.getCacheDir().getAbsolutePath() + "/" + OVPNCONFIGFILE;
    }

    static public boolean status =false;
    public static class sslForwarder implements Runnable {
        public String idName="";
        public String fakeSNI="";
        public String host="";
        public String port="";
        public String lport="";

//        class TrustAllX509TrustManager implements X509TrustManager {
//            @Override
//            public void checkClientTrusted(X509Certificate[] chain, String authType) {}
//
//            @Override
//            public void checkServerTrusted(X509Certificate[] chain, String authType) {}
//
//            @Override
//            public X509Certificate[] getAcceptedIssuers() {
//                return new X509Certificate[0];
//            }
//        }
//        TrustManager[] trustAllCerts = new TrustManager[] { new TrustAllX509TrustManager() };
//        public void setSNIHost(final SSLSocketFactory factory, final SSLSocket socket, final String hostname) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                Log.i("ssl", "Setting SNI via SSLParameters");
//                SNIHostName sniHostName = new SNIHostName(hostname);
//                SSLParameters sslParameters = socket.getSSLParameters();
//                List<SNIServerName> sniHostNameList = new ArrayList<>(1);
//                sniHostNameList.add(sniHostName);
//                sslParameters.setServerNames(sniHostNameList);
//                socket.setSSLParameters(sslParameters);
//            } else if (factory instanceof SSLCertificateSocketFactory) {
//                Log.i("ssl", "Setting SNI via SSLCertificateSocketFactory");
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//                    ((SSLCertificateSocketFactory)factory).setHostname(socket, hostname);
//                }
//            } else {
//                Log.i("ssl", "Setting SNI via reflection");
//                try {
//                    socket.getClass().getMethod("setHostname", String.class).invoke(socket, hostname);
//                } catch (Throwable e) {
//                    Log.e("ssl", "Could not call SSLSocket#setHostname(String) method ", e);
//                }
//            }
//        }
        public void run() {
            // code to be executed in the new thread
            // Create a new SSL socket factory with the all-trusting manager
            try {
            Log.d("I", "Thread started");
//                Log.d("W", "Host: " + host);
//                Log.d("W", "Port: " + port);
//                SSLContext sslContext = SSLContext.getInstance("TLS");
//                sslContext.init(null, trustAllCerts, new SecureRandom());
//                SSLSocketFactory factory = sslContext.getSocketFactory();
                int listenPort = Integer.parseInt(lport);
                String forwardHost = host;
                int forwardPort = Integer.parseInt(port);
                // Create a new SSL socket to the server
                ServerSocket serverSocket = new ServerSocket(listenPort);

//                URL url = new URL("https://mega.nz");
//                Proxy proxyt = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 5892));
//                URLConnection connection = url.openConnection(proxyt);
//                connection.connect();
//                int responseCode = ((HttpURLConnection) connection).getResponseCode();
//                if (responseCode == 200) {
//                    // Connection successful
//                    Log.d("W", "tested.");
//                } else {
//                    // Connection failed
//                    Log.d("W", "test failed.");
//                }
               serverSocket.setSoTimeout(10);
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        new Thread(() -> {
                            try {
                                Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 11000));
                                Socket forwardSocket = new Socket(proxy);
                                InetSocketAddress destinationAddress = new InetSocketAddress("127.0.0.1", 10000);
                                forwardSocket.connect(destinationAddress);
                                //Socket forwardSocket = new Socket(forwardHost, forwardPort);
//                                SSLSocket forwardSocket = (SSLSocket) factory.createSocket(forwardHost, forwardPort);
//                                setSNIHost(factory,forwardSocket,fakeSNI);
//                                forwardSocket.setEnabledCipherSuites(new String[]{"TLS_AES_128_GCM_SHA256"});
//                                forwardSocket.setEnabledProtocols(new String[]{"TLSv1.3"});
//                                forwardSocket.startHandshake();
                                while (true){
                                    if (forwardSocket.isConnected())
                                        break;
                                    SystemClock.sleep(10);
                                }
                                if (forwardSocket.isConnected())
                                    Log.d("I", "connected");
                                else
                                    Log.d("I", "Not connected");

                                String input = idName;
                                MessageDigest md = MessageDigest.getInstance("SHA-1");
                                byte[] hash = md.digest(input.getBytes("ASCII"));
                                String base64 = Base64.encodeToString(hash, Base64.NO_WRAP);
                                base64 = base64 + "\n";
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                    forwardSocket.getOutputStream().write(base64.getBytes(StandardCharsets.US_ASCII));
                                }
                                byte[] by=new byte[1024];

                                forwardSocket.getInputStream().read(by);
                                new Thread(new Pipe(clientSocket.getInputStream(), forwardSocket.getOutputStream(), clientSocket, forwardSocket)).start();
                                new Thread(new Pipe(forwardSocket.getInputStream(), clientSocket.getOutputStream(), forwardSocket, clientSocket)).start();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (NoSuchAlgorithmException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    } catch (SocketTimeoutException e) {

                    }

                }
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static class Pipe implements Runnable {
    InputStream in;
    OutputStream out;
    Socket sin;
    Socket sout;
    Pipe(InputStream in, OutputStream out,Socket Sin,Socket Sout) {
        this.in = in;
        this.out = out;
        this.sin = Sin;
        this.sout = Sout;
    }

    public void run() {
        try {
                    byte[] buffer = new byte[10240];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                sin.close();
                sout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    }
    }




