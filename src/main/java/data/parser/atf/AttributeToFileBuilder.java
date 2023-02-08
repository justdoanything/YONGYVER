package data.parser.atf;

import data.exception.ParseException;
import data.template.FileTemplate;
import data.util.FileUtil;

import java.io.FileNotFoundException;
import java.nio.file.FileSystemException;

import static data.constant.FileConstant.FILE_EXTENSION_CSV;

public class AttributeToFileBuilder extends FileTemplate {

    String getReadFilePath() {
        return readFilePath;
    }

    String getWriteFilePath() {
        return writeFilePath;
    }

    boolean isWriteFile() {
        return isWriteFile;
    }

    boolean isOpenFile() {
        return isOpenFile;
    }

    boolean isGetString() {
        return isGetString;
    }

    String getSplitter() {
        return splitter;
    }

    int getStartWithLine() {
        return startWithLine;
    }

    public AttributeToFileBuilder(String readFilePath) {
        this.readFilePath = readFilePath;
    }

    public AttributeToFileBuilder writeFilePath(String writeFilePath) {
        this.writeFilePath = writeFilePath;
        return this;
    }

    public AttributeToFileBuilder isWriteFile(boolean isWriteFile) {
        this.isWriteFile = isWriteFile;
        return this;
    }

    public AttributeToFileBuilder isOpenFile(boolean isOpenFile) {
        this.isOpenFile = isOpenFile;
        return this;
    }

    public AttributeToFileBuilder isGetString(boolean isGetString) {
        this.isGetString = isGetString;
        return this;
    }

    public AttributeToFileBuilder splitter(String splitter) {
        this.splitter = splitter;
        return this;
    }

    public AttributeToFileBuilder startWithLine(int startWithLine) {
        this.startWithLine = startWithLine;
        return this;
    }

    public AttributeToFile build() throws FileSystemException, FileNotFoundException {

        validParameter();

        return new AttributeToFile(this);
    }
}

