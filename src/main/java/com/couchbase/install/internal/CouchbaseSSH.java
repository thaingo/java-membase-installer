package com.couchbase.install.internal;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.install.exception.SSHException;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class CouchbaseSSH {
	private static final Logger LOG = LoggerFactory.getLogger(CouchbaseSSH.class);
	private JSch jsch;
	private Session session;
	private Channel channel;
	private InputStream in;
	private InputStream err;
	
	private String host;
	private String user;
	private String password;
	
	public CouchbaseSSH(String user, String host) throws SSHException {
		this(user, null, host);
	}
	
	public CouchbaseSSH(String user, String password, String host) throws SSHException {
		this.user = user;
		this.password = password;
		this.host = host;
		this.jsch = new JSch();
		
		try {
			jsch.setKnownHosts("/Users/mikewied/.ssh/known_hosts");
		} catch (JSchException e) {
			LOG.error("Unable to get known hosts file");
			throw new SSHException("Couldn't find known hosts file");
		}
		createSession();
	}
	
	private void createSession() throws SSHException {
		try {
			session = jsch.getSession(user, host, 22);
			if (password != null) {
				session.setPassword(password);
			}
			session.connect();
		} catch (JSchException e) {
			LOG.error("Unable to create SSH Session");
			throw new SSHException("Couldn't create ssh session with " + host);
		}
	}
	
	public String docommand(String command) {
		String response = null;
		
		try {
			channel = session.openChannel("exec");
			((ChannelExec)channel).setCommand(command + "\n");
			
			in = channel.getInputStream();
			err = channel.getExtInputStream();
			
			channel.connect();
			response = read();
			channel.disconnect();
		} catch (JSchException e) {
			LOG.error("Error creating SSH channel");
		} catch (IOException e) {
			LOG.error("Error handling socket streams");
		}
		return response;
	}
	
	private String read() {
		String sin =  "\n\nstdin:\n";
		String serr = "\n\nstderr:\n";
		try {
			byte[] tmp = new byte[1024];
			while (true) {

				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);
					if (i < 0)
						break;
					sin += new String(tmp, 0, i);
				}
				
				while (err.available() > 0) {
					int i = err.read(tmp, 0, 1024);
					if (i < 0)
						break;
					serr += new String(tmp, 0, i);
				}

				if (channel.isClosed()) {
					break;
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					LOG.info("Interrupted while reading. Trying again");
				}
			}
		} catch (IOException e) {
			LOG.error("IO Stream closed while reading");
		}
		return sin + serr;
	}
	
	public void closeSession() {
		session.disconnect();
	}
}
