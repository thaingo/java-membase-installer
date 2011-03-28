package com.couchbase.install.exception;

public class SSHException extends Exception {
	private static final long serialVersionUID = 1255733856909878467L;

	public SSHException() {
        super();
    }
	
	public SSHException(String message) {
        super(message);
    }
}
