/*
 * Copyright (C) 2007-2008 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.zlibrary.text.model;

import java.lang.ref.WeakReference;
import java.io.*;
import java.util.*;
import org.geometerplus.zlibrary.core.util.*;

final class CachedCharStorageException extends RuntimeException {
	public CachedCharStorageException(String message) {
		super(message);
	}
}

final class CachedCharStorage implements CharStorage {
	private final int myBlockSize;
	private final ArrayList<WeakReference<char[]> > myArray = new ArrayList<WeakReference<char[]> >(1024);
	private final String myFileNamePrefix;

	CachedCharStorage(int blockSize, String fileNamePrefix) {
		myBlockSize = blockSize;
		myFileNamePrefix = fileNamePrefix;
	}

	public int size() {
		return myArray.size();
	}

	public char[] block(int index) {
		char[] block = myArray.get(index).get();
		if (block == null) {
			try {
				File file = new File(myFileNamePrefix + index);
				int size = (int)file.length();
				if (size < 0) {
					throw new CachedCharStorageException("Error during reading " + myFileNamePrefix + index);
				}
				android.util.Log.w("file size = ", "" + size);
				block = new char[size / 2];
				InputStreamReader reader =
					new InputStreamReader(
						new FileInputStream(file),
						"UTF-16LE"
					);
				if (reader.read(block) != block.length) {
					throw new CachedCharStorageException("Error during reading " + myFileNamePrefix + index);
				}
				reader.close();
			} catch (FileNotFoundException e) {
				throw new CachedCharStorageException("Error during reading " + myFileNamePrefix + index);
			} catch (IOException e) {
				throw new CachedCharStorageException("Error during reading " + myFileNamePrefix + index);
			}
			myArray.set(index, new WeakReference<char[]>(block));
		}
		return block;
	}

	public char[] createNewBlock(int minimumLength) {
		int blockSize = myBlockSize;
		if (minimumLength > blockSize) {
			blockSize = minimumLength;
		}
		char[] block = new char[blockSize];
		myArray.add(new WeakReference(block));
		return block;
	}

	public void freezeLastBlock() {
		int index = myArray.size() - 1;
		if (index >= 0) {
			char[] block = myArray.get(index).get();
			if (block == null) {
				throw new CachedCharStorageException("Block reference in null during freeze");
			}
			try {
				OutputStreamWriter writer =
					new OutputStreamWriter(
						new FileOutputStream(myFileNamePrefix + index),
						"UTF-16LE"
					);
				writer.write(block);
				writer.close();
			} catch (IOException e) {
				throw new CachedCharStorageException("Error during writing " + myFileNamePrefix + index);
			}
		}
	}

	public void clear() {
		myArray.clear();
	}
}