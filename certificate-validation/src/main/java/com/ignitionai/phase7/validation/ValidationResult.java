package com.ignitionai.phase7.validation;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {
    private boolean isValid = true;
    private List<String> errors = new ArrayList<>();

    public void addError(String error) {
        this.isValid = false;
        this.errors.add(error);
    }

    public boolean isValid() {
        return isValid;
    }

    public List<String> getErrors() {
        return errors;
    }
}
