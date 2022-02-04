package no.ntnu.iir.bluej.extensions.linting.sonarlint.checker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;

public class ClientInputFileAdapter implements ClientInputFile {
  private File file;

  public ClientInputFileAdapter(File file) {
    this.file = file;
  }

  @Override
  public String contents() throws IOException {
    return Files.readString(this.file.toPath());
  }

  @Override
  public Charset getCharset() {
    return StandardCharsets.UTF_8;
  }

  @Override
  public <G> G getClientObject() {
    return null;
  }

  @Override
  public String getPath() {
    return this.file.getPath();
  }

  @Override
  public InputStream inputStream() throws IOException {
    return new FileInputStream(this.file);
  }

  @Override
  public boolean isTest() {
    return false;
  }

  @Override
  public String relativePath() {
    return this.file.toPath().toString();
  }

  @Override
  public URI uri() {
    return this.file.toURI();
  }


}
