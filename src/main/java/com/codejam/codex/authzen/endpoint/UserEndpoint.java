package com.codejam.codex.authzen.endpoint;

import com.codejam.codex.authzen.dtos.inputs.UpdateUserRequest;
import com.codejam.codex.authzen.dtos.outputs.UpdateUserResponse;
import com.codejam.codex.authzen.dtos.outputs.UserResponse;
import com.codejam.codex.authzen.models.User;
import com.codejam.codex.authzen.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Endpoint responsible for user-related operations such as fetching and updating profiles.
 */
@Component
public class UserEndpoint {

    private final UserService userService;

    @Autowired
    public UserEndpoint(UserService userService) {
        this.userService = userService;
    }

    /**
     * Fetches the profile of the user by username.
     *
     * @param username Username of the user
     * @return UserResponse DTO containing profile data
     */
    public UserResponse getProfile(String username) {
        return userService.getProfile(username);
    }

    /**
     * Updates the user profile based on the given request.
     *
     * @param username       Username of the user
     * @param updateRequest  Data to update
     */
    public UpdateUserResponse updateUser(String username, UpdateUserRequest updateRequest) {
        return userService.updateUser(username, updateRequest);
    }
}