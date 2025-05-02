package com.codejam.codex.authzen.responses;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class AuthzenResponse<T> {

    private static final String SUCCESSFUL = "successful";
    private static final String UNSUCCESSFUL = "unsuccessful";

    private String status;

    @Setter
    private List<T> results;

    @Setter
    private String message;

    /**
     * Default constructor, initializes with a "successful" status and an empty results list.
     */
    public AuthzenResponse() {
        this.status = SUCCESSFUL;
        this.results = new ArrayList<>();
    }

    /**
     * Constructor that initializes with a provided data object.
     *
     * @param data The data object to include in the results.
     */
    public AuthzenResponse(T data) {
        this();
        addResult(data);
    }

    /**
     * Constructor that initializes with a status and a message.
     *
     * @param results The list of results to return.
     * @param successful Whether the operation was successful or not.
     * @param message The message to include with the response.
     */
    public AuthzenResponse(List<T> results, boolean successful, String message) {
        this.status = successful ? SUCCESSFUL : UNSUCCESSFUL;
        this.results = results != null ? results : new ArrayList<>();
        this.message = message;
    }

    /**
     * Adds a data object to the results list if it is not null.
     *
     * @param data The data object to add to the results.
     */
    private void addResult(T data) {
        if (data != null) {
            results.add(data);
        }
    }

    /**
     * Adds multiple data objects to the results list.
     *
     * @param data The collection of data objects to add to the results.
     */
    public void addResults(List<T> data) {
        if (data != null) {
            this.results.addAll(data);
        }
    }

    /**
     * Sets the status to unsuccessful.
     */
    public void markUnsuccessful() {
        this.status = UNSUCCESSFUL;
    }
}
