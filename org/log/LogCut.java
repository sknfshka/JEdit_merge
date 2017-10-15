package org.log;

public class LogCut extends LogEdit {

	private int start;
	
	public LogCut() {
		super();
	}

	public LogCut(final String text) {
		super(text);
		type = LogEventTypes.CUT_ACTION;
	}
	
	public LogCut(final String text, final int start) {
		super(text);
		type = LogEventTypes.CUT_ACTION;
		this.start = start;
	}
	
	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	@Override
	public String getStringForm() {
		return super.getStringForm();
	}
}
