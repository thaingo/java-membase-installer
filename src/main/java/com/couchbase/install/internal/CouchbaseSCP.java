package com.couchbase.install.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

public class CouchbaseSCP {
	private static final Logger LOG = LoggerFactory.getLogger(CouchbaseSCP.class);
	private JSch jsch;
	private Session session;
	private String user;
	private String password;
	private String host;
	
	public CouchbaseSCP(String user, String password, String host) {
		this.user = user;
		this.password = password;
		this.host = host;
		this.jsch = new JSch();
		createSession();
	}
	
	private void createSession() {
		try {
			session = jsch.getSession(user, host, 22);
			UserInfo ui = new MyUserInfo(user, password, password);
			session.setUserInfo(ui);
			session.connect();
		} catch (JSchException e) {
			LOG.error("Unable to create SCP session");
		}
	}

	public void doSCP(String lfile, String rfile) {
		FileInputStream fis = null;
		
		if (!session.isConnected()) {
			createSession();
		}

		try {
			String command = "scp -p -t " + rfile;
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

			channel.connect();

			if (checkAck(in) != 0) {
				System.exit(0);
			}

			// send "C0644 filesize filename", where filename should not include
			// '/'
			long filesize = (new File(lfile)).length();
			command = "C0644 " + filesize + " ";
			if (lfile.lastIndexOf('/') > 0) {
				command += lfile.substring(lfile.lastIndexOf('/') + 1);
			} else {
				command += lfile;
			}
			command += "\n";
			
			out.write(command.getBytes());
			out.flush();
			
			if (checkAck(in) != 0) {
				System.exit(0);
			}

			// send a content of lfile
			fis = new FileInputStream(lfile);
			byte[] buf = new byte[1024];
			while (true) {
				int len = fis.read(buf, 0, buf.length);
				if (len <= 0)
					break;
				out.write(buf, 0, len); // out.flush();
			}
			fis.close();
			fis = null;
			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
			if (checkAck(in) != 0) {
				System.exit(0);
			}
			out.close();
			channel.disconnect();
		} catch (Exception e) {
			System.out.println(e);
			try {
				if (fis != null)
					fis.close();
			} catch (Exception ee) {
			}
		}
	}

	private int checkAck(InputStream in) throws IOException {
		int b = in.read();

		if (b == 0)
			return b;
		if (b == -1)
			return b;
		if (b == 1 || b == 2) {
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char) c);
			} while (c != '\n');
			if (b == 1) { // error
				System.out.print(sb.toString());
			}
			if (b == 2) { // fatal error
				System.out.print(sb.toString());
			}
		}
		return b;
	}
	
	public void closeSession() {
		session.disconnect();
	}
}

final class MyUserInfo implements UserInfo {
	private final String user_;
	private final String password_;
	private final String passphrase_;

	public MyUserInfo(String user, String password, String passphrase) {
		user_ = user;
		password_ = password;
		passphrase_ = passphrase;
	}

	public String getUser() {
		return user_;
	}

	public String getPassphrase() {
		return passphrase_;
	}

	public String getPassword() {
		return password_;
	}

	public boolean promptPassphrase(String message) {
		return true;
	}

	public boolean promptPassword(String message) {
		return true;
	}

	public boolean promptYesNo(String message) {
		return true;
	}

	public void showMessage(String message) {
	}

}
