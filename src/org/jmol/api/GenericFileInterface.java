package javajs.api;

public interface GenericFileInterface {

  String getFullPath();

  String getName();

  long length();

  boolean isDirectory();

  GenericFileInterface getParentAsFile();

}
