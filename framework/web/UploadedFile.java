package framework.web;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class UploadedFile {

    private String originalFilename;
    private String contentType;
    private byte[] content;

    public UploadedFile(String originalFilename, String contentType, byte[] content) {
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.content = content;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getContent() {
        return content;
    }

    public long getSize() {
        return content != null ? content.length : 0;
    }

    public boolean isEmpty() {
        return content == null || content.length == 0;
    }

    public void saveTo(String directory) throws IOException {
        File dir = new File(directory);
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, originalFilename);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content);
        }
    }
}
