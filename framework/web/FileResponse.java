package framework.web;

public class FileResponse {
    private byte[] content;
    private String contentType;
    private String fileName;

    public FileResponse(byte[] content, String contentType, String fileName) {
        this.content = content;
        this.contentType = contentType;
        this.fileName = fileName;
    }

    public byte[] getContent() {
        return content;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFileName() {
        return fileName;
    }
}
