package jspecview.api;

import javax.swing.JFrame;

import jspecview.common.PrintLayout;

public interface JSVPrintDialog {

	JSVPrintDialog set(JFrame parentFrame, PrintLayout lastPrintLayout, boolean isJob);

	PrintLayout getPrintLayout();

}
