package org.log;

public class LogCompile extends LogItem {

	private String text;
	
	public LogCompile() {
		this.type = LogEventTypes.COMPILE_ACTION;
	}
	
	public LogCompile(String text) {
		this.text = text;
		this.type = LogEventTypes.COMPILE_ACTION;
	}
	
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@Override
	public String getStringForm() {
		return super.getStringForm();
	}
}
