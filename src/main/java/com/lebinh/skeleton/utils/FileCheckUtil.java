package com.lebinh.skeleton.utils;

import java.io.File;
import java.util.Arrays;
import liquibase.util.file.FilenameUtils;

public class FileCheckUtil {

  public static boolean checkFileExtension(File file, String... allowExtensions) {
    String fileExtension = FilenameUtils.getExtension(file.getName());

    boolean result =
        Arrays.asList(allowExtensions).stream().anyMatch(ext -> ext.equals(fileExtension));

    return result;
  }

}
