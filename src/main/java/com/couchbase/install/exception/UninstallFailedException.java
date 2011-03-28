package com.couchbase.install.exception;

public class UninstallFailedException extends Exception{
	private static final long serialVersionUID = 545587689544294224L;

	public UninstallFailedException() {
        super();
    }
	
	public UninstallFailedException(String message) {
        super(message);
    }
}
