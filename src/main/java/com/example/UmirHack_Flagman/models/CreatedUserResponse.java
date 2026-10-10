package com.example.UmirHack_Flagman.models;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CreatedUserResponse {
    private UserResponse user;
    private InviteResponse invite;
}
