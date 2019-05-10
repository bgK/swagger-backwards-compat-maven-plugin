package io.kemtoa.swagger.compat.walker;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Position in a Swagger API specification document
 */
public class Location {
    private boolean isRequest;
    private boolean isResponse;
    private Deque<String> path = new ArrayDeque<>();

    public boolean isRequest() {
        return isRequest;
    }

    public boolean isResponse() {
        return isResponse;
    }

    public void setRequest(boolean request) {
        isRequest = request;
    }

    public void setResponse(boolean response) {
        isResponse = response;
    }

    public void pushPath(String name) {
        path.push(name);
    }

    public void popPath() {
        path.pop();
    }

    public String getFullLocation() {
        StringBuilder fullLocation = new StringBuilder();

        Iterator<String> iterator = path.descendingIterator();
        while (iterator.hasNext()) {
            fullLocation.append(iterator.next());

            if (iterator.hasNext()) {
                fullLocation.append(", ");
            }
        }

        return fullLocation.toString();
    }
}
