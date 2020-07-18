package com.lebinh.skeleton.web.rest.vm;

import com.lebinh.skeleton.service.dto.UserDto;
import javax.validation.constraints.Size;

/**
 * View Model extending the UserDto, which is meant to be used in the user management UI.
 */
public class ManagedUserVm extends UserDto {

    public static final int PASSWORD_MIN_LENGTH = 4;

    public static final int PASSWORD_MAX_LENGTH = 100;

    @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH)
    private String password;

    public ManagedUserVm() {
        // Empty constructor needed for Jackson.
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "ManagedUserVm{" +
            "} " + super.toString();
    }
}
