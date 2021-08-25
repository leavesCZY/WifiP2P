package github.leavesc.wifip2p.task;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;

import github.leavesc.wifip2p.common.Constants;
import github.leavesc.wifip2p.model.FileTransfer;
import github.leavesc.wifip2p.util.Md5Util;

/**
 * @Author: leavesC
 * @Date: 2019/2/27 23:56
 * @Desc: 客户端发送文件
 * @Github：https://github.com/leavesC
 */
public class WifiClientTask extends AsyncTask<Object, Integer, Boolean> {

    private static final String TAG = "WifiClientTask";

    private final ProgressDialog progressDialog;

    @SuppressLint("StaticFieldLeak")
    private final Context context;

    public WifiClientTask(Context context) {
        this.context = context.getApplicationContext();
        progressDialog = new ProgressDialog(context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setTitle("正在发送文件");
        progressDialog.setMax(100);
    }

    @Override
    protected void onPreExecute() {
        progressDialog.show();
    }

    private String getOutputFilePath(Uri fileUri) throws Exception {
        String outputFilePath = context.getExternalCacheDir().getAbsolutePath() +
                File.separatorChar + new Random().nextInt(10000) +
                new Random().nextInt(10000) + ".jpg";
        File outputFile = new File(outputFilePath);
        if (!outputFile.exists()) {
            outputFile.getParentFile().mkdirs();
            outputFile.createNewFile();
        }
        Uri outputFileUri = Uri.fromFile(outputFile);
        copyFile(context, fileUri, outputFileUri);
        return outputFilePath;
    }

    @Override
    protected Boolean doInBackground(Object... params) {
        Socket socket = null;
        OutputStream outputStream = null;
        ObjectOutputStream objectOutputStream = null;
        InputStream inputStream = null;
        try {
            String hostAddress = params[0].toString();
            Uri imageUri = Uri.parse(params[1].toString());

            String outputFilePath = getOutputFilePath(imageUri);
            File outputFile = new File(outputFilePath);

            FileTransfer fileTransfer = new FileTransfer();
            String fileName = outputFile.getName();
            String fileMa5 = Md5Util.getMd5(outputFile);
            long fileLength = outputFile.length();
            fileTransfer.setFileName(fileName);
            fileTransfer.setMd5(fileMa5);
            fileTransfer.setFileLength(fileLength);

            Log.e(TAG, "文件的MD5码值是：" + fileTransfer.getMd5());

            socket = new Socket();
            socket.bind(null);
            socket.connect((new InetSocketAddress(hostAddress, Constants.PORT)), 10000);
            outputStream = socket.getOutputStream();
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(fileTransfer);
            inputStream = new FileInputStream(outputFile);
            long fileSize = fileTransfer.getFileLength();
            long total = 0;
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
                total += len;
                int progress = (int) ((total * 100) / fileSize);
                publishProgress(progress);
                Log.e(TAG, "文件发送进度：" + progress);
            }
            socket.close();
            inputStream.close();
            outputStream.close();
            objectOutputStream.close();
            socket = null;
            inputStream = null;
            outputStream = null;
            objectOutputStream = null;
            Log.e(TAG, "文件发送成功");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "文件发送异常 Exception: " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (objectOutputStream != null) {
                try {
                    objectOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    private void copyFile(Context context, Uri inputUri, Uri outputUri) throws NullPointerException,
            IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(inputUri);
             OutputStream outputStream = new FileOutputStream(outputUri.getPath())) {
            if (inputStream == null) {
                throw new NullPointerException("InputStream for given input Uri is null");
            }
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        progressDialog.setProgress(values[0]);
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        progressDialog.cancel();
        Log.e(TAG, "onPostExecute: " + aBoolean);
    }

}
