package com.chasen.wifitransfer.model;

import java.io.Serializable;

/**
 * 传输的文件实体类
 *
 * @Author Chasen
 * @Data 2018/10/9
 */

public class FileInfo implements Serializable {

    /**
     * 文件传输结果：1 成功  -1 失败
     */
    public static final int FLAG_SUCCESS = 1;
    public static final int FLAG_FAILURE = -1;

    // 文件名
    private String fileName;
    // 文件路径
    private String path;
    // 文件大小
    private long size;
    // 文件类型
    private int fileType;
    // 传输结果
    private int result;

    public static int getFlagSuccess() {
        return FLAG_SUCCESS;
    }

    public static int getFlagFailure() {
        return FLAG_FAILURE;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getFileType() {
        return fileType;
    }

    public void setFileType(int fileType) {
        this.fileType = fileType;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "FileInfo{" +
                "fileName='" + fileName + '\'' +
                ", path='" + path + '\'' +
                ", size=" + size +
                ", fileType=" + fileType +
                ", result=" + result +
                '}';
    }
}
