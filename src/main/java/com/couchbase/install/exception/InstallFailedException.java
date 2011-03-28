package com.couchbase.install.exception;

public class InstallFailedException extends Exception {
	private static final long serialVersionUID = 1612092419020233049L;

	public InstallFailedException() {
        super();
    }
	
	public InstallFailedException(String message) {
        super(message);
    }
}
