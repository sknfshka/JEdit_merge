package org.log;

public class LogRun extends LogItem {

	private String text;
	
	public LogRun() {
		type = LogEventTypes.RUN_ACTION;
	}
	
	public LogRun(String text) {
		this.text = text;
		type = LogEventTypes.RUN_ACTION;
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
