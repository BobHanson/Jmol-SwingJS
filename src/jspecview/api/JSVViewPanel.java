package jspecview.api;

public interface JSVViewPanel {
	void dispose();
  int getHeight();
  String getTitle();
  int getWidth();
  boolean isEnabled();
  boolean isFocusable();
  boolean isVisible();
  void setEnabled(boolean b);
	void setFocusable(boolean b);
  void setTitle(String title);
}
