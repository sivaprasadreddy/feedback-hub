package dev.sivalabs.speakup.users;

record CreateUserCmd(String name, String email, String password, Role role) {}
