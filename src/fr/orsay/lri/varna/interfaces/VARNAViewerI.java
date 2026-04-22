package fr.orsay.lri.varna.interfaces;

import java.util.Map;

import javax.swing.JFrame;

import fr.orsay.lri.varna.VARNAPanel;

public interface VARNAViewerI {

  VARNAPanel getVarnaPanel();

  String getStruct();

  String getSeq();

  String getInfo();

  JFrame getFrame();

  JFrame setFrame(JFrame parentFrame, JFrame frame, int width, int height);

  void newDSSRSequenceAndStructure(String modelName, Map<String, Object> dssrInfo);

}
