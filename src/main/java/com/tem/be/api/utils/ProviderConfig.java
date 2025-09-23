package com.tem.be.api.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ProviderConfig {

    @JsonProperty("expectedHeaders")
    private List<String> expectedHeaders;
}
