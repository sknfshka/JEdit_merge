package org.log;

import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.codehaus.jackson.map.ObjectMapper;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtil {
	private static final Logger log = LoggerFactory.getLogger(LogUtil.class);
	private static ObjectMapper mapper = new ObjectMapper();
	
	public static void logKeyPressed(KeyEvent evt, TextArea textArea) {
		int caret = textArea.getCaretPosition();
		
		if(isPrintableKey(evt)) {
			final LogCharacterKey key = new LogCharacterKey(evt.getKeyChar(), caret, evt.getKeyCode(), evt.getModifiers());
			try {
				log.info(mapper.writeValueAsString(key));
			} catch (Exception ex) {
				Log.log(1, null, "cannot write json:\n", ex);
			}
		} else if(isServiceKey(evt)) {
		if(isKeyForSelection(evt)) {
				return;
			}

			final LogServiceKey key = new LogServiceKey(evt.getKeyCode(), caret, evt.getModifiers());
			try {
				log.info(mapper.writeValueAsString(key));
			} catch (Exception ex) {
				Log.log(1, null, "cannot write json:\n", ex);
			}
		}
		else if(isDeletedKey(evt)) {
			final LogDelete key;
			String deletedText;

			if(isBackspaceKey(evt)) {
				deletedText = getDeletedText(true, textArea);
				key = new LogDelete(evt.getKeyCode(), caret, evt.getModifiers(), deletedText, getDeletedCharPosition(true, textArea));
			} else {
				deletedText = getDeletedText(false, textArea);
				key = new LogDelete(evt.getKeyCode(), caret, evt.getModifiers(), deletedText, getDeletedCharPosition(false, textArea));
			}
			try {
				log.info(mapper.writeValueAsString(key));
			} catch (Exception ex) {
				Log.log(1, null, "cannot write json:\n", ex);
			}
		}
	}
	
	private static int getDeletedCharPosition(boolean isBackspace, TextArea textArea) {
		int selectionCount = textArea.getSelectionCount();
		if(selectionCount == 0) {
			if(isBackspace)
				return textArea.getCaretPosition() - 1;
			else
				return textArea.getCaretPosition();
		} else {
			return textArea.getSelection(selectionCount - 1).getStart();
		}
			
	}
	
	private static String getDeletedText(boolean isBackspace, TextArea textArea) {
		String selection = textArea.getSelectedText();
		int caret = textArea.getCaretPosition();
		
		if(selection == null || selection.isEmpty()) {
			if(isBackspace)
				return textArea.getText(caret - 1, 1);
			return textArea.getText(caret, 1);
		}
		
		return selection;
	}
	
	private static boolean isAltMask(final KeyEvent e) {
		return (e.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) == KeyEvent.ALT_DOWN_MASK;
	}

	private static boolean isCtrlMask(final KeyEvent e) {
		return (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) == KeyEvent.CTRL_DOWN_MASK;
	}

	private static boolean isPrintableKey(final KeyEvent e) {
		if (isAltMask(e) || isCtrlMask(e)) {
			return false;
		}
		final int keyCode = e.getKeyCode();
		if (keyCode >= KeyEvent.VK_COMMA && keyCode <= KeyEvent.VK_9) {
			return true;
		}
		if (keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_CLOSE_BRACKET) {
			return true;
		}
		return keyCode == KeyEvent.VK_SEMICOLON
			|| keyCode == KeyEvent.VK_EQUALS
			|| keyCode == KeyEvent.VK_QUOTE
			|| keyCode == KeyEvent.VK_BACK_QUOTE
			|| keyCode == KeyEvent.VK_ENTER
			|| keyCode == KeyEvent.VK_TAB
			|| keyCode == KeyEvent.VK_SPACE;
	}
	
	private static boolean isKeyForSelection(final KeyEvent event) {
		int keyCode = event.getKeyCode();
		return (keyCode >= KeyEvent.VK_LEFT && keyCode<= KeyEvent.VK_DOWN) && event.getModifiers() == 1;
	}
	
	private static boolean isDeleteKey(final KeyEvent event) {
		return event.getKeyCode() == KeyEvent.VK_DELETE;
	}
	
	private static boolean isBackspaceKey(KeyEvent event) {
		return event.getKeyCode() == KeyEvent.VK_BACK_SPACE;
	}

	private static boolean isDeletedKey(KeyEvent event) {
		int keyCode = event.getKeyCode();
		return keyCode == KeyEvent.VK_BACK_SPACE
			|| keyCode == KeyEvent.VK_DELETE;
	}
	
	private static boolean isServiceKey(final KeyEvent event) {
		if (isCtrlMask(event) || isAltMask(event)) {
			return false;
		}
		int keyCode = event.getKeyCode();
		return (keyCode >= KeyEvent.VK_LEFT && keyCode<= KeyEvent.VK_DOWN)
				//|| keyCode == KeyEvent.VK_TAB
				//|| keyCode == KeyEvent.VK_ENTER
				//|| keyCode == KeyEvent.VK_BACK_SPACE
				//|| keyCode == KeyEvent.VK_DELETE
				|| keyCode == KeyEvent.VK_CAPS_LOCK
				|| keyCode == KeyEvent.VK_INSERT;
	}

	public static void compileCSourceFile(final Buffer toCompile, final JEditTextArea textArea) {
		if (toCompile.isDirty()) {
			toCompile.save(textArea.getView(), toCompile.getPath());
		}

		final File output = Paths.get("out", "compileOut").toFile();
		final File error = Paths.get("out", "compileError").toFile();
		ArrayList<String> commands = new ArrayList<>();
		commands.add(jEdit.getProperty("c.compiler"));
		commands.add(toCompile.getName());
		try {
			String s;
			Files.copy(Paths.get(toCompile.getPath()), Paths.get("out", toCompile.getName()), StandardCopyOption.REPLACE_EXISTING);
			final Process process = new ProcessBuilder(commands).directory(Paths.get("out").toFile()).redirectOutput(output).redirectError(error).start();
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				s = getContentOfFile(error);
				JOptionPane.showMessageDialog(textArea, s, "Result of compilation", JOptionPane.ERROR_MESSAGE);
			} else {
				s = "Compilation was successful";
				JOptionPane.showMessageDialog(textArea, s, "Result of compilation", JOptionPane.INFORMATION_MESSAGE);
			}

			try {
				log.info(mapper.writeValueAsString(new LogCompile(s)));
			} catch (Exception e) {
				Log.log(Log.ERROR, null, "Cannot write copy action to json", e);
			}

			Files.deleteIfExists(output.toPath());
			Files.deleteIfExists(error.toPath());
		} catch (IOException e) {
			JOptionPane.showMessageDialog(textArea, "Cannot open file for output of compiler");
		} catch (InterruptedException e) {
			JOptionPane.showMessageDialog(textArea, "Compiling was interrupted");
		}
	}

	public static void runCSourceFile(final Buffer toRun, final JEditTextArea textArea) throws IOException {
		final File output = Paths.get("out", "runOut").toFile();
		final File error = Paths.get("out", "runError").toFile();
		ArrayList<String> commands = new ArrayList<>();
		commands.add(Paths.get("out").toUri().getPath() + "a.exe");

		try {
			String s;
			final Process process = new ProcessBuilder(commands).redirectOutput(output).redirectError(error).start();
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				s = getContentOfFile(error);
				JOptionPane.showMessageDialog(textArea, s, "Result of running", JOptionPane.ERROR_MESSAGE);
			} else {
				s = getContentOfFile(output);
				JOptionPane.showMessageDialog(textArea, s, "Result of running", JOptionPane.INFORMATION_MESSAGE);
			}
			try {
				log.info(mapper.writeValueAsString(new LogRun(s)));
			} catch (Exception e) {
				Log.log(Log.ERROR, null, "Cannot write copy action to json", e);
			}
			Files.deleteIfExists(output.toPath());
			Files.deleteIfExists(error.toPath());
		} catch (IOException e) {
			JOptionPane.showMessageDialog(textArea, "Cannot open file for output of compiler");
		} catch (InterruptedException e) {
			JOptionPane.showMessageDialog(textArea, "Run was interrupted");
		}
	}
	
	public static void compileBuffer(final Buffer toCompile, final JEditTextArea textArea) {
		if (toCompile.isDirty()) {
            toCompile.save(textArea.getView(), toCompile.getPath());
        }

        final File output = Paths.get("out", "compileOut").toFile();
        final File error = Paths.get("out", "compileError").toFile();
        ArrayList<String> commands = new ArrayList<>();
        commands.add(jEdit.getProperty("java.compiler"));
        commands.add(toCompile.getName());
        try {
            String s;
            Files.copy(Paths.get(toCompile.getPath()), Paths.get("out", toCompile.getName()), StandardCopyOption.REPLACE_EXISTING);
            final Process process = new ProcessBuilder(commands).directory(Paths.get("out").toFile()).redirectOutput(output).redirectError(error).start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                s = getContentOfFile(error);
                JOptionPane.showMessageDialog(textArea, s, "Result of compilation", JOptionPane.ERROR_MESSAGE);
            } else {
                s = "Compilation was successful";
                JOptionPane.showMessageDialog(textArea, s, "Result of compilation", JOptionPane.INFORMATION_MESSAGE);
            }

            try {
                log.info(mapper.writeValueAsString(new LogCompile(s)));
            } catch (Exception e) {
                Log.log(Log.ERROR, null, "Cannot write copy action to json", e);
            }

            Files.deleteIfExists(output.toPath());
            Files.deleteIfExists(error.toPath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(textArea, "Cannot open file for output of compiler");
        } catch (InterruptedException e) {
            JOptionPane.showMessageDialog(textArea, "Compiling was interrupted");
        }
	}
	
	public static void runBuffer(final Buffer toRun, final JEditTextArea textArea) throws IOException {
		final File output = Paths.get("out", "runOut").toFile();
        final File error = Paths.get("out", "runError").toFile();
        ArrayList<String> commands = new ArrayList<>();
        commands.add(jEdit.getProperty("java.start"));
        commands.add(toRun.getName().replace(".java", ""));

        try {
            String s;
            final Process process = new ProcessBuilder(commands).directory(Paths.get("out").toFile()).redirectOutput(output).redirectError(error).start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                s = getContentOfFile(error);
                JOptionPane.showMessageDialog(textArea, s, "Result of running", JOptionPane.ERROR_MESSAGE);
            } else {
                s = getContentOfFile(output);
                JOptionPane.showMessageDialog(textArea, s, "Result of running", JOptionPane.INFORMATION_MESSAGE);
            }
            try {
                log.info(mapper.writeValueAsString(new LogRun(s)));
            } catch (Exception e) {
                Log.log(Log.ERROR, null, "Cannot write copy action to json", e);
            }
            Files.deleteIfExists(output.toPath());
            Files.deleteIfExists(error.toPath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(textArea, "Cannot open file for output of compiler");
        } catch (InterruptedException e) {
            JOptionPane.showMessageDialog(textArea, "Run was interrupted");
        }
	}
	
	private static String getContentOfFile(final File file) throws IOException {
		BufferedReader br = Files.newBufferedReader(file.toPath(), Charset.defaultCharset());
		final StringBuffer res = new StringBuffer();
		String s;
		while ((s = br.readLine()) != null) {
			res.append(s);
            res.append("\n");
		}
		br.close();
		return res.toString();
	}
}
