package leavesc.hello.filetransfer.model;

import androidx.annotation.NonNull;

import java.io.Serializable;

/**
 * 作者：leavesC
 * 时间：2019/2/27 23:52
 * 描述：
 * GitHub：https://github.com/leavesC
 * Blog：https://www.jianshu.com/u/9df45b87cfdf
 */
public class FileTransfer implements Serializable {

    //文件路径
    private String filePath;

    //文件大小
    private long fileLength;

    //MD5码
    private String md5;

    public FileTransfer(String name, long fileLength) {
        this.filePath = name;
        this.fileLength = fileLength;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileLength() {
        return fileLength;
    }

    public void setFileLength(long fileLength) {
        this.fileLength = fileLength;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    @NonNull
    @Override
    public String toString() {
        return "FileTransfer{" +
                "filePath='" + filePath + '\'' +
                ", fileLength=" + fileLength +
                ", md5='" + md5 + '\'' +
                '}';
    }

}