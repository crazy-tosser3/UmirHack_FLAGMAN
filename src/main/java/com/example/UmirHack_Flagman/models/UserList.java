package com.example.UmirHack_Flagman.models;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserList {
    private List<UserResponse> items;
    private int page;
    private int limit;
    private long total;
}
