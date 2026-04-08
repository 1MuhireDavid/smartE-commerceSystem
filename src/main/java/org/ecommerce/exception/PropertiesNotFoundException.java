package org.ecommerce.exception;

public class PropertiesNotFoundException extends RuntimeException {
    public PropertiesNotFoundException(String message) {
        super(message);
    }
    public PropertiesNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }


}
