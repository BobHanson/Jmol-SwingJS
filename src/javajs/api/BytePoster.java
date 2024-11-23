package javajs.api;

public interface BytePoster {

  /**
   * 
   * @param fileName
   * @param bytes
   * @return null or a Java exception message if there is an error; otherwise "OK" and a message
   */
  String postByteArray(String fileName, byte[] bytes);

}
