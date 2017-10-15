package org.log.parse;

import org.apache.bsf.util.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.gjt.sp.jedit.bsh.StringUtil;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.util.Log;
import org.log.LogCharacterKey;
import org.log.LogCloseFile;
import org.log.LogCompile;
import org.log.LogCopy;
import org.log.LogCut;
import org.log.LogDelete;
import org.log.LogEventTypes;
import org.log.LogItem;
import org.log.LogKey;
import org.log.LogOpenFile;
import org.log.LogPaste;
import org.log.LogRun;
import org.log.LogSaveFile;
import org.log.LogSelection;
import org.log.LogSelectionClear;
import org.log.LogServiceKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AWTException;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class ParseUtil {
	private static final Logger log = LoggerFactory.getLogger(ParseUtil.class);
	private static ObjectMapper mapper = new ObjectMapper();;
	
	private static ArrayList<ArrayList<LogItem>> allItems = null;
	//private static ArrayList<LogItem> currentGroupSameItems;
	private static int iterator = -2;
	private static JEditTextArea textArea = null;
	private static JEditBuffer buffer;
	//private static LogItem current;
	
	public static void openActionListView() {
		if(allItems == null || allItems.isEmpty() || textArea == null) {
			JOptionPane.showMessageDialog(textArea, "Not open org.log file", "Message", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		ViewMain viewMain = new ViewMain(allItems, textArea);	
	}
	
	public static void parseLog(JEditTextArea area, JEditBuffer buf) {
		try {
			buffer = buf;
			textArea = area;
			if (openLogFile()) {
				iterator = 0;
			}
			else
				log.info("Error open File");
		} catch (Exception ex) {
			Log.log(Log.ERROR, textArea, "Something went wrong", ex);
		}	
	}
	
	public static void parseFile(final String filename) throws Exception {
		allItems = new ArrayList<ArrayList<LogItem>>();
		ArrayList<LogItem> sameItemType = new ArrayList<LogItem>();
		BufferedReader br = null;
		try {
			br = Files.newBufferedReader(Paths.get(filename), Charset.defaultCharset());
			String s;
			LogEventTypes lastAddedItemType = null;
			sameItemType = new ArrayList<LogItem>();

			while((s = br.readLine()) != null) {
				String jsonString = getJSONSubstring(s);

				if(!jsonString.isEmpty()) {
					LogItem item = readOneObject(jsonString);
					LogEventTypes type = item.getType();

					if(type == lastAddedItemType || lastAddedItemType == null) {
						sameItemType.add(item);
						lastAddedItemType = type;
					} else {
						addSameItemsToCollection(sameItemType, lastAddedItemType);
						sameItemType = new ArrayList<LogItem>();
						sameItemType.add(item);
						lastAddedItemType = type;
					}
				}
			}
			if (!sameItemType.isEmpty()) {
				addSameItemsToCollection(sameItemType, lastAddedItemType);
			}
		} finally {

			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					System.out.println("Cannot close reader");
				}
			}
		}
	}

	private static void addSameItemsToCollection(ArrayList<LogItem> sameItemType, LogEventTypes lastAddedItemType) {
		if(lastAddedItemType == LogEventTypes.SELECTION) {
            LogItem lastAddedItem = sameItemType.get(sameItemType.size() - 1);
            sameItemType = new ArrayList<LogItem>();
            sameItemType.add(lastAddedItem);
        }

		allItems.add(sameItemType);
	}

	private static String getJSONSubstring(String stringFromLog) {
		String jsonString = "";
		int begin = stringFromLog.indexOf("{");
		int end = stringFromLog.lastIndexOf("}");

		if(begin != -1 && end != -1) {
			jsonString = stringFromLog.substring(begin, end + 1);
		}

		return jsonString;
	}
	
	private static LogItem readOneObject(String jsonString) {
		if(jsonString.isEmpty()) {
			return null;
		}

        LogItem item = null;

        try {
			item = mapper.readValue(jsonString, LogItem.class);

			if(item == null) {
				return item;
			}

			item = getLogItem(jsonString, item.getType());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return item;
    }
	
	public static void nextAction(boolean isSkip) {
		if(iterator == -1) {
			log.info("End of file");
			return;
		}

		displayAction(isSkip);
		if(iterator >= allItems.size())
			iterator = -1;
		else
			++iterator;
	}

	private static LogItem getLogItem(final String source, final LogEventTypes type) throws IOException {
		switch (type) {
			case CHARACTER_KEY:
				return mapper.readValue(source.getBytes(), LogCharacterKey.class);
			case SAVE_ACTION:
				return mapper.readValue(source.getBytes(), LogSaveFile.class);
			case SELECTION:
				return mapper.readValue(source.getBytes(), LogSelection.class);
			case SELECTION_CLEAR:
				return mapper.readValue(source.getBytes(), LogSelectionClear.class);
			case SERVICE_KEY:
				return mapper.readValue(source.getBytes(), LogServiceKey.class);
			case OPEN_ACTION:
				return mapper.readValue(source.getBytes(), LogOpenFile.class);
			case CLOSE_ACTION:
				return mapper.readValue(source.getBytes(), LogCloseFile.class);
			case COMPILE_ACTION:
				return mapper.readValue(source.getBytes(), LogCompile.class);
			case RUN_ACTION:
				return mapper.readValue(source.getBytes(), LogRun.class);
			case PASTE_ACTION:
				return mapper.readValue(source.getBytes(), LogPaste.class);
			case COPY_ACTION:
				return mapper.readValue(source.getBytes(), LogCopy.class);
			case CUT_ACTION:
				return mapper.readValue(source.getBytes(), LogCut.class);
			case DELETE_KEY:
				return mapper.readValue(source.getBytes(), LogDelete.class);
			default:
				throw new IllegalArgumentException("there is no such type");
		}
	}
	
	private static boolean openLogFile() throws Exception {
		JFileChooser chooser = new JFileChooser(Paths.get("logs").toFile());
		if (chooser.showOpenDialog(textArea) == JFileChooser.APPROVE_OPTION) {
			ParseUtil.parseFile(chooser.getSelectedFile().getAbsolutePath());
			return true;
		} else {
			return false;
		}
	}
	
	private static void displayAction(boolean isSkip) {
		log.info("IN DISPLAY ACTION. ITERATOR: " + iterator);
		ArrayList<LogItem> sameItems = allItems.get(iterator);
		LogEventTypes type = sameItems.get(0).getType();
		
		switch(type) {
			case SERVICE_KEY :
				pressServiceKey(sameItems);
				break;

			case DELETE_KEY:
				pressDeleteKey(sameItems);
				break;
				
			case CHARACTER_KEY :
				pressCharKey(sameItems);
				break;
				
			case SELECTION :
				LogSelection selection = (LogSelection)sameItems.get(sameItems.size() - 1);
				textArea.setCaretPosition(selection.getEnd());
				textArea.setSelection(selection.createSelection());				
				break;
				
			case CUT_ACTION :
				LogCut cut = (LogCut)sameItems.get(0);
				simulateCutAction(cut);
				if(!isSkip) {
					JOptionPane.showMessageDialog(textArea, cut.getText(), "Cut action", JOptionPane.INFORMATION_MESSAGE);
				}
				break;
				
			case PASTE_ACTION :
				LogPaste paste = (LogPaste) sameItems.get(0);
				LogItem previousItem;
				ArrayList<LogItem> previousSameItems;
				if(iterator != 0) {
					previousSameItems = allItems.get(iterator - 1);
					previousItem = previousSameItems.get(previousSameItems.size() - 1);
					simulatePasteAction(paste, previousItem, isSkip);
				} else
					simulatePasteAction(paste, null, isSkip);
				
				break;
				
			case COPY_ACTION :
				if(!isSkip) {
					LogCopy copy = (LogCopy)sameItems.get(0);
					JOptionPane.showMessageDialog(textArea, copy.getText(), "Copy action", JOptionPane.INFORMATION_MESSAGE);
				}
				break;
				
			case COMPILE_ACTION :
				if(!isSkip) {
					LogCompile compile = (LogCompile)sameItems.get(0);
					JOptionPane.showMessageDialog(textArea, compile.getText(), "Compile action", JOptionPane.INFORMATION_MESSAGE);
				}
				break;
				
			case RUN_ACTION :
				if(!isSkip) {
					LogRun run = (LogRun)sameItems.get(0);
					JOptionPane.showMessageDialog(textArea, run.getText(), "Run action", JOptionPane.INFORMATION_MESSAGE);
				}
				break;
				
			case SAVE_ACTION :
				if(!isSkip) {
					LogSaveFile save = (LogSaveFile)sameItems.get(0);
					JOptionPane.showMessageDialog(textArea, save.getPath(), "Save action", JOptionPane.INFORMATION_MESSAGE);
				}
				break;
				
			default :
				log.info("No this type: " + type);	
		}
	}
	
	public static void previousAction(boolean isSkip) {
		if(iterator == -1)
			iterator = allItems.size() - 1;
		else if(iterator <= 0) {
			log.info("Start file");
			return;
		}
		
		--iterator;
		deleteAction(isSkip);
	}
	
	private static void deleteAction(boolean isSkip) {
		ArrayList<LogItem> sameItems = allItems.get(iterator);
		LogEventTypes type = sameItems.get(0).getType();
		
		switch(type) {
			case SERVICE_KEY :
				deleteServiceKey(sameItems);
				break;

			case DELETE_KEY:
				deleteDeleteKey(sameItems);
				break;
				
			case CHARACTER_KEY :
				deleteCharKey(sameItems);
				break;
				
			case SELECTION :
				LogSelection selection = (LogSelection)sameItems.get(sameItems.size() - 1);
				textArea.setCaretPosition(selection.getEnd());
				textArea.setCaretPosition(selection.getStart());				
				break;
				
			case CUT_ACTION :			
				deleteCutAction(sameItems, isSkip);			
				break;
				
			case PASTE_ACTION :
				LogPaste paste = (LogPaste) sameItems.get(0);
				LogItem previousItem;
				ArrayList<LogItem> previousSameItems;
				if(iterator != 0) {
					previousSameItems = allItems.get(iterator - 1);
					previousItem = previousSameItems.get(previousSameItems.size() - 1);
					deletePasteAction(paste, previousItem, isSkip);
				} else
					deletePasteAction(paste, null, isSkip);
				
				break;
				
			case COPY_ACTION :
				if(!isSkip) {
					LogCopy copy = (LogCopy)sameItems.get(0);
					JOptionPane.showMessageDialog(textArea, copy.getText(), "Copy action", JOptionPane.INFORMATION_MESSAGE);
				}
				break;
				
			case COMPILE_ACTION :
				if(!isSkip) {
					LogCompile compile = (LogCompile)sameItems.get(0);
					JOptionPane.showMessageDialog(textArea, compile.getText(), "Compile action", JOptionPane.INFORMATION_MESSAGE);
				}
				break;
				
			case RUN_ACTION :
				if(!isSkip) {
					LogRun run = (LogRun)sameItems.get(0);
					JOptionPane.showMessageDialog(textArea, run.getText(), "Run action", JOptionPane.INFORMATION_MESSAGE);
				}
				break;
				
			case SAVE_ACTION :
				if(!isSkip) {
					LogSaveFile save = (LogSaveFile)sameItems.get(0);
					JOptionPane.showMessageDialog(textArea, save.getPath(), "Save action", JOptionPane.INFORMATION_MESSAGE);
				}
				break;
		}
	}
	
	private static void pressServiceKey(ArrayList<LogItem> serviceItems) {
		LogServiceKey servItem = (LogServiceKey)serviceItems.get(serviceItems.size() - 1);
		textArea.setCaretPosition(getCaretForLogKey(servItem));
	}

	private static void pressDeleteKey(ArrayList<LogItem> deleteItems) {
		LogDelete delItem = (LogDelete)deleteItems.get(deleteItems.size() - 1);
		for(LogItem item : deleteItems) {
			LogDelete serviceItem = (LogDelete)item;
			textArea.setCaretPosition(getCaretForLogKey(delItem));
			if(isDeletedKey(serviceItem.getKeyCode()))
				buffer.remove(serviceItem.getDeletedCharPosition(), serviceItem.getDeletedText().length());
		}
	}

	public static boolean isDeletedKey(final int keyCode) {
		return keyCode == KeyEvent.VK_DELETE || keyCode == KeyEvent.VK_BACK_SPACE;
	}
	
	private static void pressCharKey(ArrayList<LogItem> charItems) {	
		int counter = 0;
		for(LogItem item : charItems) {		
			LogCharacterKey charItem = (LogCharacterKey)item;
			buffer.insert(charItem.getPosition(), "" + charItem.getKeyChar());
			textArea.setCaretPosition(charItem.getPosition() + 1);
		}
	}
	
	private static boolean isShiftRequired(LogCharacterKey item) {
		return item.getMask() == KeyEvent.SHIFT_MASK;
	}
	
	private static int getCaretForLogKey(LogKey item)
	{
		if (buffer.getLength() < item.getPosition()) {
			return buffer.getLength();
		} else {
			return item.getPosition();
		}
	}
	
	private static void simulateCutAction(LogCut item) {
		buffer.remove(item.getStart(), item.getText().length());
		textArea.setCaretPosition(item.getStart());
	}
	
	private static void simulatePasteAction(LogPaste item, LogItem previousItem, boolean isSkip) {
		if(previousItem != null && previousItem.getType().equals(LogEventTypes.SELECTION))
			buffer.remove(((LogSelection)previousItem).getStart(), ((LogSelection)previousItem).getSelectedText().length());

		buffer.insert(item.getPosition(), item.getText());
		textArea.setCaretPosition(item.getPosition() + item.getText().length());
		
		if(!isSkip)
			JOptionPane.showMessageDialog(textArea, item.getText(), "Paste action", JOptionPane.INFORMATION_MESSAGE);
	}
	
	private static void deleteCutAction(ArrayList<LogItem> sameItems, boolean isSkip) {
		LogCut cut = (LogCut)sameItems.get(0);
		int startSelection = cut.getStart();
		int endSelection = startSelection + cut.getText().length();
		
		buffer.insert(startSelection, cut.getText());
		textArea.setSelection(new Selection.Range(startSelection, endSelection));
		if(!isSkip)
			JOptionPane.showMessageDialog(textArea, cut.getText(), "Cut action", JOptionPane.INFORMATION_MESSAGE);
	}
	
	private static void deletePasteAction(LogPaste item, LogItem previousItem, boolean isSkip) {
		buffer.remove(item.getPosition(), item.getText().length());
		
		if(previousItem != null && previousItem.getType().equals(LogEventTypes.SELECTION)) {
			int start = ((LogSelection)previousItem).getStart();
			int end = ((LogSelection)previousItem).getEnd();
			String text = ((LogSelection)previousItem).getSelectedText();
			buffer.insert(start, text);
			textArea.setSelection(new Selection.Range(start, end));
		}
		
		if(!isSkip)
			JOptionPane.showMessageDialog(textArea, item.getText(), "Paste action", JOptionPane.INFORMATION_MESSAGE);
	}
	
	private static void deleteCharKey(ArrayList<LogItem> sameItems) {
		int length = sameItems.size();
		int start = ((LogCharacterKey)sameItems.get(0)).getPosition();
		buffer.remove(start, length);
		if (buffer.getLength() < start)
			textArea.setCaretPosition(buffer.getLength());
		else
			textArea.setCaretPosition(start);
	}
	
	private static void deleteServiceKey(ArrayList<LogItem> sameItems) {
		LogServiceKey item = (LogServiceKey)sameItems.get(0);

		if (buffer.getLength() < item.getPosition())
			textArea.setCaretPosition(buffer.getLength());
		else
			textArea.setCaretPosition(item.getPosition());
	}

	private static void deleteDeleteKey(ArrayList<LogItem> sameItems) {
		int size = sameItems.size();
		LogDelete item = (LogDelete)sameItems.get(0);
		for(int i = size-1; i >= 0; i--) {
			LogDelete servItem = (LogDelete)sameItems.get(i);

			int start = servItem.getDeletedCharPosition();
			String deletedText = servItem.getDeletedText();
			int end = start + deletedText.length();
			buffer.insert(start, deletedText);
			if(deletedText.length() > 1)
				textArea.setSelection(new Selection.Range(start, end));
		}
	}
	
	public static int getIterator() {
		return iterator;
	}
	
}
