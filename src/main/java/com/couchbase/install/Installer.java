package com.couchbase.install;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.install.exception.InstallFailedException;
import com.couchbase.install.exception.SSHException;
import com.couchbase.install.exception.UninstallFailedException;
import com.couchbase.install.internal.CouchbaseSCP;
import com.couchbase.install.internal.CouchbaseSSH;

public class Installer {
	private static final Logger LOG = LoggerFactory.getLogger(Installer.class);
	private static final String WLOC = "http://builds.hq.northscale.net/releases/1.6.5/";
	private static final String LLOC = "http://builds.hq.northscale.net/latestbuilds/";	
	private String user;
	private String pass;
	private String host;
	private String sVersion;
	private String mVersion;
	
	
	public Installer(String user, String pass, String host, String sVersion, String mVersion) {
		this.user = user;
		this.pass = pass;
		this.host = host;
		this.sVersion = sVersion;
		this.mVersion = mVersion;
	}
	
	public void installMoxi() throws InstallFailedException {
		try {
			CouchbaseSSH ssh = new CouchbaseSSH(user, pass, host);
			ssh.docommand("rm -rf " + sVersion + "*");
			if (!ssh.docommand("wget " + LLOC + mVersion).contains("`" + mVersion + "' saved")) {
				ssh.docommand("rm -rf " + mVersion + "*");
				ssh.closeSession();
				throw new InstallFailedException("Unable to download Moxi: Bad version");
			}
			if (sVersion.endsWith(".deb")) {
				if (!ssh.docommand("dpkg -i " + mVersion).contains("Setting up moxi-server")) {
					ssh.docommand("rm -rf " + mVersion + "*");
					ssh.closeSession();
					throw new InstallFailedException("Debian install failed on host: " + host);
				}
			} else if (sVersion.endsWith(".rpm")) {
				if (!ssh.docommand("rpm -i " + mVersion).contains("You have successfully installed Moxi Server.")) {
					ssh.docommand("rm -rf " + mVersion + "*");
					ssh.closeSession();
					throw new InstallFailedException("RedHat install failed on host: " + host);
				}
			} else if (mVersion.endsWith(".exe")) {
				ssh.closeSession();
				throw new InstallFailedException("Windows installation not currently supported");
			} else {
				ssh.closeSession();
				throw new InstallFailedException("Error installing" + sVersion + " on " + host + ". Doesn't appear to be linux package");
			}
			ssh.docommand("rm -rf " + mVersion + "*");
			ssh.closeSession();
		} catch (SSHException e) {
			throw new InstallFailedException();
		}
	}
	
	public void installClient() throws InstallFailedException {
		CouchbaseSSH ssh = null;
		CouchbaseSCP scp = null;
		try {
			ssh = new CouchbaseSSH(user, pass, host);
			scp = new CouchbaseSCP(user, pass, host);
			scp.doSCP("distributed_loadgen.tar.gz", "distributed_loadgen.tar.gz");
			ssh.docommand("tar xf distributed_loadgen.tar.gz");
			ssh.docommand("rm -f distributed_loadgen.tar.gz");
			if (!ssh.docommand("cd distributed_loadgen; ant;").contains("BUILD SUCCESSFUL")) {
				ssh.closeSession();
				throw new InstallFailedException("Load Generator didn't compile correctly on " + host);
			}
			ssh.docommand("cd distributed_loadgen; ant run > /dev/null &");
		} catch (SSHException e) {
			throw new InstallFailedException("Couldn't create ssh session with " + host);
		} finally {
			if (scp != null) {
				scp.closeSession();
			}
			if (ssh != null) {
				ssh.closeSession();
			}
		}
	}
	
	public void installServer() throws InstallFailedException {
		CouchbaseSSH ssh = null;
		try {
			ssh = new CouchbaseSSH(user, pass, host);
			ssh.docommand("rm -rf " + sVersion + "*");
			if (!ssh.docommand("wget " + WLOC + sVersion).contains("`" + sVersion + "' saved")) {
				ssh.docommand("rm -rf " + sVersion + "*");
				ssh.closeSession();
				throw new InstallFailedException("Unable to download Membase: Bad version");
			}
			
			if (sVersion.endsWith(".deb")) {
				if (!ssh.docommand("dpkg -i " + sVersion).contains("* Started Membase server")) {
					ssh.docommand("rm -rf " + sVersion + "*");
					ssh.closeSession();
					throw new InstallFailedException("Debian install failed on host: " + host);
				}
			} else if (sVersion.endsWith(".rpm")) {
				if (!ssh.docommand("rpm -i " + sVersion).contains("You have successfully installed Membase Server.")) {
					ssh.docommand("rm -rf " + sVersion + "*");
					ssh.closeSession();
					throw new InstallFailedException("RedHat install failed on host: " + host);
				}
			} else if (sVersion.endsWith(".exe")) {
				ssh.closeSession();
				throw new InstallFailedException("Windows installation not currently supported");
			} else {
				ssh.closeSession();
				throw new InstallFailedException("Error installing" + sVersion + " on " + host + ". Doesn't appear to be linux package");
			}
			ssh.docommand("rm -rf " + sVersion + "*");
			ssh.closeSession();
		} catch (SSHException e) {
			throw new InstallFailedException();
		} finally {
			if (ssh != null) {
				ssh.closeSession();
			}
		}
	}
	
	public void uninstallLinux() throws UninstallFailedException{
		CouchbaseSSH ssh;
		try {
			ssh = new CouchbaseSSH(user, pass, host);
		} catch (SSHException e) {
			throw new UninstallFailedException();
		}
		if (sVersion.endsWith(".deb")) {
			ssh.docommand("dpkg -r membase-server");
		} else if (sVersion.endsWith(".rpm")) {
			ssh.docommand("rpm -e membase-server");
		} else if (sVersion.endsWith(".exe")) {
			LOG.info("No Windows uninstall support");
		}
		ssh.docommand("rm -rf /var/opt/membase");
		ssh.docommand("rm -rf /etc/opt/membase");
		ssh.docommand("rm -rf /opt/membase*");
		ssh.closeSession();
	}
	
	public void uninstallClient() throws UninstallFailedException{
		CouchbaseSSH ssh;
		try {
			ssh = new CouchbaseSSH(user, pass, host);
		} catch (SSHException e) {
			throw new UninstallFailedException();
		}		ssh.docommand("pkill java");
		ssh.docommand("rm -rf distributed_loadgen");
		ssh.closeSession();
	}
	
	public void uninstallMoxi() throws UninstallFailedException{
		CouchbaseSSH ssh;
		try {
			ssh = new CouchbaseSSH(user, pass, host);
		} catch (SSHException e) {
			throw new UninstallFailedException();
		}		if (sVersion.endsWith(".deb")) {
			ssh.docommand("dpkg -r moxi-server");
		} else if (sVersion.endsWith(".rpm")) {
			ssh.docommand("rpm -e moxi-server");
		} else if (sVersion.endsWith(".exe")) {
			LOG.info("No Windows uninstall support");
		}
		ssh.closeSession();
	}
	
	public static void main(String args[]) {		
		Installer installer = new Installer("root", "northscale!23", "10.2.1.54", "membase-server-enterprise_x86_64_1.6.5.deb", "moxi-server_x86_64.deb");
		try {
			installer.installMoxi();
		} catch (InstallFailedException e) {
			System.out.println("Install failed: " + e.getMessage());
		}
		//installer.uninstallMoxi();
	}
}
