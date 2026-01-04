package framework.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MultipleFiles {

    private List<UploadedFile> files = new ArrayList<>();

    public MultipleFiles() {}

    public void addFile(UploadedFile file) {
        if (file != null && !file.isEmpty()) {
            files.add(file);
        }
    }

    public List<UploadedFile> getFiles() {
        return files;
    }

    public boolean isEmpty() {
        return files.isEmpty();
    }

    public void saveAll(String directory) throws IOException {
        for (UploadedFile file : files) {
            file.saveTo(directory);
        }
    }
}
