package javajs.api;

/**
 * methods in JSmol JavaScript accessed in Jmol 
 */
public interface JmolObjectInterface {

  Object _doAjax(Object url, String postOut, Object bytesOrStringOut);

  void _apply(Object func, Object data);

}
