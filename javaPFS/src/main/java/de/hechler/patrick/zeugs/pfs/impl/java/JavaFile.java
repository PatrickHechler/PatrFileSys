package de.hechler.patrick.zeugs.pfs.impl.java;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import de.hechler.patrick.zeugs.pfs.interfaces.File;
import de.hechler.patrick.zeugs.pfs.interfaces.Stream;
import de.hechler.patrick.zeugs.pfs.misc.ElementType;
import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;


public class JavaFile extends JavaFSElement implements File {
	
	public JavaFile(Path root, Path path) {
		super(root, path);
	}
	
	@Override
	public long length() throws IOException {
		return Files.size(full);
	}
	
	@Override
	public void truncate(long length) throws IOException {
		try (SeekableByteChannel channel = Files.newByteChannel(full, StandardOpenOption.WRITE)) {
			channel.truncate(length);
		}
	}
	
	@Override
	public Stream open(StreamOpenOptions options) throws IOException {
		options = options.ensureType(ElementType.file);
		List<OpenOption> opts = new ArrayList<>();
		if (options.append()) {
			opts.add(StandardOpenOption.APPEND);
		} else if (options.write()) {
			opts.add(StandardOpenOption.WRITE);
		}
		if (options.read()) {
			opts.add(StandardOpenOption.READ);
		}
		if (options.createOnly()) {
			opts.add(StandardOpenOption.CREATE_NEW);
		} else if (options.createAlso()) {
			opts.add(StandardOpenOption.CREATE);
		}
		SeekableByteChannel channel = Files.newByteChannel(full, opts.toArray(new OpenOption[opts.size()]));
		return switch (options) {
		case StreamOpenOptions o when o.write() && o.read() -> new ChannelReadWriteStream(channel, options, full);
		case StreamOpenOptions o when o.write() && !o.read() -> new ChannelWriteStream(channel, options, full);
		case StreamOpenOptions o when !o.write() && o.read() -> new ChannelReadStream(channel, options, full);
		default -> throw new IllegalArgumentException("I won't open a stream without read and without write access!");
		};
	}
	
}
