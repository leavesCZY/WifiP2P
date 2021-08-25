package github.leavesc.wifip2p.model;

import java.io.Serializable;

/**
 * @Author: leavesC
 * @Date: 2019/2/27 23:52
 * @Desc:
 * @Github：https://github.com/leavesC
 */
public class FileTransfer implements Serializable {

    private String fileName;

    private long fileLength;

    private String md5;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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

}