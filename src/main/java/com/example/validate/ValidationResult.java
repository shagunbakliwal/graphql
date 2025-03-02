package com.example.validate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ValidationResult {
    private int index;
    private String inputType;
    private boolean valid;
    private String message;
    private List<String> invalidFields;
}
