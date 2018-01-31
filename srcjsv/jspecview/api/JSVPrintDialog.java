package jspecview.api;

import jspecview.common.PrintLayout;

public interface JSVPrintDialog {

	JSVPrintDialog set(Object offWindowFrame, PrintLayout lastPrintLayout, boolean isJob);

	PrintLayout getPrintLayout();

}
