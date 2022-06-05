package de.hechler.patrick.pfs.shell.objects;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class PatrCommand {
	
	public String[]     cmd  = null;
	public OutputStream out  = null;
	public OutputStream err  = null;
	public InputStream  in   = null;
	
	public PatrCommand() {}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PatrCommand [");
		if (cmd != null) {
			builder.append("cmd=");
			builder.append(Arrays.toString(cmd));
		}
		if (out != null) {
			builder.append(", delegate out");
		}
		if (err != null) {
			builder.append(", delegate err");
		}
		if (in != null) {
			builder.append(", delegate in");
		}
		builder.append("]");
		return builder.toString();
	}
	
}
