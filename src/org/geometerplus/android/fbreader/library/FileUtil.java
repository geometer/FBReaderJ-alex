package org.geometerplus.android.fbreader.library;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.resources.ZLResource;

public class FileUtil {
	
	public static void copyFile(String src, String targetDirectory) throws IOException {
		final ZLResource myResource = ZLResource.resource("libraryView");

		if (ZLFile.createFileByPath(targetDirectory).isDirectory()){
			ZLFile srcFile = ZLFile.createFileByPath(src);
			String srcName = srcFile.getShortName();
			
			for (ZLFile f : ZLFile.createFileByPath(targetDirectory).children()){
				if (f.getShortName().equals(srcName))
					throw new IOException (myResource.getResource("messFileExists").getValue());
			}
			String target = targetDirectory + "/" + srcFile.getShortName(); 
			FileChannel ic = new FileInputStream(src).getChannel();
			FileChannel oc = new FileOutputStream(target).getChannel();
			ic.transferTo(0, ic.size(), oc);
			ic.close();
			oc.close();
		}else{
			throw new IOException(myResource.getResource("messInsertIntoArchive").getValue());
		}
	}
	
	public static void moveFile(String src, String targetDirectory) throws IOException {
		copyFile(src, targetDirectory);
		ZLFile.createFileByPath(src).getPhysicalFile().delete();
	}
	
	public static boolean contain(String fileName, ZLFile parent){
		for(ZLFile f : parent.children()){
			if (f.getShortName().equals(fileName))
				return true;
		}
		return false;
	}
	
	public static ZLFile getParent(ZLFile file){
		if (file.isDirectory()){
			String path = file.getPath();
			path = path.substring(0, path.lastIndexOf("/"));
			return ZLFile.createFileByPath(path);
		}
		return ZLFile.createFileByPath(file.getParent().getPath());
	}
}