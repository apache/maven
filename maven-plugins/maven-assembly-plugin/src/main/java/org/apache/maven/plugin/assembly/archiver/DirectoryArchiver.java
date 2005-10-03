package org.apache.maven.plugin.assembly.archiver;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.codehaus.plexus.archiver.AbstractArchiver;
import org.codehaus.plexus.archiver.ArchiveEntry;
import org.codehaus.plexus.archiver.ArchiverException;

/**
 * A plexus archiver implementation that stores the files to archive in a
 * directory.
 */
public class DirectoryArchiver 
	extends AbstractArchiver 
{

	public void createArchive() 
		throws ArchiverException, IOException 
	{
		//Most of this method was copied from org.codehaus.plexus.archiver.tar.TarArchiver
		//and modified to store files in a directory, not a tar archive.
		Map listFiles = getFiles();
		if ( listFiles == null || listFiles.size() == 0 ) 
		{
			new ArchiverException( "You must set at least one file." );
		}

		File destDirectory = getDestFile();
		if ( destDirectory == null ) 
		{
			new ArchiverException( "You must set the destination directory." );
		}
		if ( destDirectory.exists() && !destDirectory.isDirectory() ) 
		{
			new ArchiverException( destDirectory + " isn't a directory." );
		}
		if ( destDirectory.exists() && !destDirectory.canWrite() ) 
		{
			new ArchiverException( destDirectory + " is read-only." );
		}

		// Check if we don't add directory file in itself
		for ( Iterator iter = getFiles().keySet().iterator(); iter.hasNext(); ) 
		{
			String fileName = ( String ) iter.next();
			ArchiveEntry fileToAdd = ( ArchiveEntry ) getFiles().get( fileName );
			if ( destDirectory.equals( fileToAdd.getFile() ) ) 
			{
				throw new ArchiverException(
						"The destination directory cannot include itself.");
			}
		}

		getLogger().info( "Building assembly directory : " + destDirectory.getAbsolutePath() );

		try 
		{
			for ( Iterator iter = getFiles().keySet().iterator(); iter.hasNext(); ) 
			{
				String fileName = ( String ) iter.next();
				ArchiveEntry f = ( ArchiveEntry ) getFiles().get( fileName );
				String destDir = destDirectory.getCanonicalPath();
				fileName = destDir + File.separator + fileName;
				copyFile( f, fileName );
			}
		}
		catch ( IOException ioe ) 
		{
			String message = "Problem copying files : " + ioe.getMessage();
			throw new ArchiverException( message, ioe );
		}
	}

	/**
	 * Copies the specified file to the specified path, creating any ancestor directory
	 * structure as necessary.
	 * 
	 * @param file The file to copy (IOException will be thrown if this does not exist)
	 * @param vPath The fully qualified path to copy the file to.
	 * @throws ArchiverException If there is a problem creating the directory structure
	 * @throws IOException If there is a problem copying the file
	 */
	protected void copyFile( ArchiveEntry entry, String vPath )
			throws ArchiverException, IOException 
	{
		// don't add "" to the archive
		if ( vPath.length() <= 0 ) 
		{
			return;
		}
		
		File inFile = entry.getFile();
		File outFile = new File( vPath );

		if ( outFile.exists() && outFile.lastModified() >= inFile.lastModified() ) 
		{
			//already up to date...
			return;
		}
		
		outFile.setLastModified( inFile.lastModified() );
		
		if ( ! inFile.isDirectory() )
		{
			if ( ! outFile.getParentFile().exists() ) 
			{
				//create the parent directory...
				if ( ! outFile.getParentFile().mkdirs() ) 
				{
					//Failure, unable to create specified directory for some unknown reason.
					throw new ArchiverException ("Unable to create directory or parent directory of "
									+ outFile );
				}
			}
			FileInputStream fIn = new FileInputStream( inFile );
			FileOutputStream fout = new FileOutputStream( outFile );
			try 
			{
				byte[] buffer = new byte[ 8 * 1024 ];
				int count = 0;
				do 
				{
					fout.write( buffer, 0, count );
					count = fIn.read( buffer, 0, buffer.length );
				} 
				while ( count != -1 );
			} 
			finally 
			{
				try 
				{
					fIn.close();
				}
				catch ( IOException ioe ) 
				{
					fout.close();
					throw ioe;
				}
				fout.close();
			}
		}
		else 
		{ //file is a directory
			if ( outFile.exists() ) 
			{
				if ( ! outFile.isDirectory() ) 
				{
					//should we just delete the file and replace it with a directory?
					//throw an exception, let the user delete the file manually.
					throw new ArchiverException(
							"Expected directory and found file at copy destination of "
									+ inFile + " to " + outFile );
				}
			} 
			else if ( ! outFile.mkdirs() ) 
			{
				//Failure, unable to create specified directory for some unknown reason.
				throw new ArchiverException(
						"Unable to create directory or parent directory of "
								+ outFile );
			}

		}
	}
}